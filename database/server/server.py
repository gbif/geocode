# Copied from https://github.com/openmaptiles/postservehttps://github.com/openmaptiles/postserve (MIT license)
#
# Changes:
# - political and eez data sources for the geocoder (GeneratePrepared)
# - use WGS 84 projection (bounds)
# - serve HTML+JS interface (m)
#
# Some code is redundant (e.g. pixel_width, scale_denominator handling).
#

import tornado.ioloop
import tornado.web
import io
import os

from sqlalchemy import Column, ForeignKey, Integer, String
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import relationship
from sqlalchemy import create_engine
from sqlalchemy import inspect
from sqlalchemy import text
from sqlalchemy.orm import sessionmaker

import mercantile
import pyproj
import yaml
import sys
import itertools

def GetTM2Source(file):
    with open(file,'r') as stream:
        tm2source = yaml.load(stream)
    return tm2source

def GeneratePrepared():
    queries = []
    prepared = "PREPARE gettile(geometry, numeric, numeric, numeric) AS "

    political_layer = { "name": "political", "query": "SELECT gid as __id__, 'political' AS type, name AS title, iso_a2 AS iso_a2, __geom__ FROM political" }

    eez_layer = { "name": "eez", "query": "SELECT eez.gid AS __id__, 'eez' AS type, geoname AS title, CONCAT(iso_map1.iso2, iso_map2.iso2, iso_map3.iso2) AS iso_a2, __geom__ FROM eez LEFT OUTER JOIN iso_map iso_map1 ON eez.iso_ter1 = iso_map1.iso3 LEFT OUTER JOIN iso_map iso_map2 ON eez.iso_ter2 = iso_map2.iso3 LEFT OUTER JOIN iso_map iso_map3 ON eez.iso_ter3 = iso_map3.iso3" }

    for layer in [political_layer, eez_layer]:
        layer_query = layer['query']
        name = layer['name']
        layer_query = layer_query.replace("__geom__", "ST_AsMVTGeom(geom,!bbox!,4096,0,true) AS mvtgeometry")
        base_query = "SELECT ST_ASMVT('"+name+"', 4096, 'mvtgeometry', tile) FROM ("+layer_query+" WHERE ST_AsMVTGeom(geom, !bbox!,4096,0,true) IS NOT NULL) AS tile"
        queries.append(base_query.replace("!bbox!","$1").replace("!scale_denominator!","$2").replace("!pixel_width!","$3").replace("!pixel_height!","$4"))

    prepared = prepared + " UNION ALL ".join(queries) + ";"
    print(prepared)
    return(prepared)

prepared = GeneratePrepared()
engine = create_engine('postgresql://'+os.getenv('POSTGRES_USER','openmaptiles')+':'+os.getenv('POSTGRES_PASSWORD','openmaptiles')+'@'+os.getenv('POSTGRES_HOST','postgres')+':'+os.getenv('POSTGRES_PORT','5432')+'/'+os.getenv('POSTGRES_DB','openmaptiles'))
inspector = inspect(engine)
DBSession = sessionmaker(bind=engine)
session = DBSession()
session.execute(prepared)

def bounds(zoom,x,y):
    map_width_in_metres = 180.0

    tiles_down = 2**(zoom)
    tiles_across = 2**(zoom)

    x = x - 2**(zoom)
    y = -(y - 2**(zoom)) - 1 - 2**(zoom-1)

    print(x, y);

    tile_width_in_metres = (map_width_in_metres / tiles_across)
    tile_height_in_metres = (map_width_in_metres / tiles_down)
    ws = (x*tile_width_in_metres, (y+1)*tile_height_in_metres)
    en = ((x+1)*tile_height_in_metres, (y)*tile_width_in_metres)

    return {'w':ws[0],'s':ws[1],'e':en[0],'n':en[1]}

def zoom_to_scale_denom(zoom):						# For !scale_denominator!
    # From https://github.com/openstreetmap/mapnik-stylesheets/blob/master/zoom-to-scale.txt
    map_width_in_metres = 2 * 2**0.5*6371007.2 # Arctic
    map_width_in_metres = 180.0
    tile_width_in_pixels = 512.0
    standardized_pixel_size = 0.00028
    map_width_in_pixels = tile_width_in_pixels*(2.0**zoom)
    return str(map_width_in_metres/(map_width_in_pixels * standardized_pixel_size))

def replace_tokens(query,s,w,n,e,scale_denom,z):
    return query.replace("!bbox!","ST_SetSRID(ST_MakeBox2D(ST_Point("+w+", "+s+"), ST_Point("+e+", "+n+")), 4326)").replace("!scale_denominator!",scale_denom).replace("!pixel_width!","512").replace("!pixel_height!","512")

def get_mvt(zoom,x,y):
    try:								# Sanitize the inputs
        sani_zoom,sani_x,sani_y = float(zoom),float(x),float(y)
        del zoom,x,y
    except:
        print('suspicious')
        return 1

    scale_denom = zoom_to_scale_denom(sani_zoom)
    tilebounds = bounds(sani_zoom,sani_x,sani_y)
    s,w,n,e = str(tilebounds['s']),str(tilebounds['w']),str(tilebounds['n']),str(tilebounds['e'])
    final_query = "EXECUTE gettile(!bbox!, !scale_denominator!, !pixel_width!, !pixel_height!);"
    sent_query = replace_tokens(final_query,s,w,n,e,scale_denom,sani_zoom)
    print('Final query', sent_query)
    response = list(session.execute(sent_query))
    print (sani_zoom, sani_x, sani_y)
    print(sent_query)
    layers = filter(None,list(itertools.chain.from_iterable(response)))
    final_tile = b''
    for layer in layers:
        final_tile = final_tile + io.BytesIO(layer).getvalue()
    return final_tile

class GetTile(tornado.web.RequestHandler):
    def get(self, zoom,x,y):
        self.set_header("Content-Type", "application/x-protobuf")
        self.set_header("Content-Disposition", "attachment")
        self.set_header("Access-Control-Allow-Origin", "*")
        response = get_mvt(zoom,x,y)
        self.write(response)

def m():
    if __name__ == "__main__":
        # Make this prepared statement from the tm2source
        application = tornado.web.Application(
            [
                (r"/tiles/([0-9]+)/([0-9]+)/([0-9]+).pbf", GetTile),
                (r"/(.*\.?[hjp].*)", tornado.web.StaticFileHandler, {"path": "/server/static", "default_filename": "index.html"})
            ],
            {"static_path": "/server/static"}
        )
        print("Postserve started..")
        application.listen(8080)
        tornado.ioloop.IOLoop.instance().start()

m()
