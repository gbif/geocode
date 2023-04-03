proj4.defs('EPSG:4326', "+proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees");

var pixel_ratio = parseInt(window.devicePixelRatio) || 1;

var extent = 180.0;
var tile_size = 512;
var resolutions = Array(15).fill().map((_, i) => ( extent / tile_size / Math.pow(2, i) ));

var tileGrid = new ol.tilegrid.TileGrid({
    extent: ol.proj.get('EPSG:4326').getExtent(),
    minZoom: 0,
    maxZoom: 16,
    resolutions: resolutions,
    tileSize: 512,
});

// This is for the bitmap cache, just use a single rectangular 'tile'.
var tileGridBitmapCache = new ol.tilegrid.TileGrid({
    extent: ol.proj.get('EPSG:4326').getExtent(),
    minZoom: 0,
    maxZoom: 16,
    resolutions: [extent / 3600],
    tileSize: [7200, 3600],
});

var layers = [];
//const urlBase = 'https://api.gbif-dev.org/v1/geocode';
const urlBase = '..';
const occurrences_density_url = 'https://api.gbif.org/v2/map/occurrence/density/{z}/{x}/{y}@{r}x.png?srs=EPSG%3A4326'.replace('{r}', pixel_ratio);
const occurrences_adhoc_url = 'https://api.gbif.org/v2/map/occurrence/adhoc/{z}/{x}/{y}@{r}x.png?srs=EPSG%3A4326'.replace('{r}', pixel_ratio);

var labels_input = document.getElementById('labels_input');

String.prototype.hashCode = function() {
    var hash = 5381;
    for (var i = 0; i < this.length; i++) {
        hash = ((hash << 5) + hash) + this.charCodeAt(i);
    }
    return hash;
};

function countryColour(isocode) {
    var longer = (isocode+isocode+isocode+isocode+isocode+'');
    var hash = longer.hashCode();
    var r = (hash & 0xFF0000) >> 16;
    var g = (hash & 0x00FF00) >> 8;
    var b = hash & 0x0000FF;
    return "rgb(" + r + "," + g + "," + b + ")";
}
function oppositeCountryColour(isocode) {
    var longer = (isocode+isocode+isocode+isocode+isocode+'');
    var hash = longer.hashCode();
    var r = (255 - hash & 0xFF0000) >> 16;
    var g = (255 - hash & 0x00FF00) >> 8;
    var b = 255 - hash & 0x0000FF;
    return "rgb(" + r + "," + g + "," + b + ")";
}

function makePatternF(colour) {
    var cnv = document.createElement('canvas');
    var ctx = cnv.getContext('2d');
    cnv.width = 8;
    cnv.height = 8;
    ctx.fillStyle = colour;

    for(var i = 0; i < cnv.width; i += 1) {
        ctx.fillRect(cnv.width-1-i, i, 1, 1);
    }

    return ctx.createPattern(cnv, 'repeat');
}

function makePatternB(colour) {
    var cnv = document.createElement('canvas');
    var ctx = cnv.getContext('2d');
    cnv.width = 8;
    cnv.height = 8;
    ctx.fillStyle = colour;

    for(var i = 0; i < cnv.width; i += 1) {
        ctx.fillRect(i, i, 1, 1);
    }

    return ctx.createPattern(cnv, 'repeat');
}

function makePatternBBlob(colour) {
    var cnv = document.createElement('canvas');
    var ctx = cnv.getContext('2d');
    cnv.width = 14;
    cnv.height = 14;
    ctx.fillStyle = colour;

    for(var i = 0; i < 10; i += 2) {
        ctx.fillRect(i, i, 1, 1);
        ctx.fillRect(i+1, i+1, 1, 1);
        ctx.fillRect(i+3, i+3, 1, 1);
        ctx.fillRect(i+4, i+4, 4, 4);
    }

    return ctx.createPattern(cnv, 'repeat');
}

function makePatternH(colour) {
    var cnv = document.createElement('canvas');
    var ctx = cnv.getContext('2d');
    cnv.width = 8;
    cnv.height = 8;
    ctx.fillStyle = colour;

    for(var i = 0; i < cnv.width; i += 4) {
        ctx.fillRect(i, 0, cnv.width, 1);
    }

    return ctx.createPattern(cnv, 'repeat');
}

function countryStyle() {
    var styles = [];
    return function(feature, resolution) {
        var defaultFill = new ol.style.Fill({color: '#22bbff'});

        var point = new ol.style.Style({
            image: new ol.style.Circle({
                fill: defaultFill,
                radius: 5
            }),
            fill: defaultFill
        });

        var text = new ol.style.Style({text: new ol.style.Text({
            text: '',
            fill: defaultFill,
            font: '13px "Open Sans", "Arial Unicode MS"',
            stroke: new ol.style.Stroke({color: '#d0d0d0', width: 3}),
        })});

        var length = 0;
        var isocode = feature.get('isocountrycode2digit') ? feature.get('isocountrycode2digit') : feature.get('id');
        if (feature.get('type').startsWith('GADM')) {
            isocode = feature.get('id');
        }
        var colour = countryColour(isocode);
        var invertColour = oppositeCountryColour(isocode);

        console.log(feature.get('type'));

        if (feature.get('type') == 'Political') {
            pattern = makePatternBBlob(colour);
        } else if (feature.get('type') == 'WGSRPD') {
            pattern = makePatternB(colour);
        } else if (feature.get('type') == 'GADM3' || feature.get('type') == 'IHO') {
            pattern = makePatternH(colour);
        } else {
            pattern = makePatternF(colour);
        }

        if (feature.getType() == 'Point') {
            var featureFill = new ol.style.Fill({color: colour});

            text.getText().setFill(featureFill);
            if (labels_input.checked) {
                text.getText().setText(stringDivider(feature.get('title'), 16, '\n'));
                text.getText().getStroke(); // .setColor(invertColour);
                styles[length++] = text;
            }

            styles[length++] = new ol.style.Style({
                image: new ol.style.Circle({
                    fill: text.getText().getFill(),
                    stroke: new ol.style.Stroke({color: invertColour, width: 1}),
                    radius: 2.5
                }),
            });
        } else {
            styles[length++] = new ol.style.Style({
                fill: new ol.style.Fill({color: pattern}),
                stroke: new ol.style.Stroke({color: colour})
            });
        }
        styles.length = length;
        return styles;
    };
}

// http://stackoverflow.com/questions/14484787/wrap-text-in-javascript
function stringDivider(str, width, spaceReplacer) {
    if (!str) return str;
    if (str.length > width) {
        var p = width;
        while (p > 0 && (str[p] != ' ' && str[p] != '-')) {
            p--;
        }
        if (p > 0) {
            var left;
            if (str.substring(p, p + 1) == '-') {
                left = str.substring(0, p + 1);
            } else {
                left = str.substring(0, p);
            }
            var right = str.substring(p + 1);
            return left + spaceReplacer + stringDivider(right, width, spaceReplacer);
        }
    }
    return str;
}

var view = new ol.View({
    center: [0, 20],
    projection: 'EPSG:4326',
    zoom: 2,
    resolutions: resolutions
});

layers['grid'] = new ol.layer.Tile({
    title: 'Tile grid',
	extent: ol.proj.get('EPSG:4326').getExtent(),
	source: new ol.source.TileDebug({
		projection: 'EPSG:4326',
		tileGrid: tileGrid,
		wrapX: false
	}),
	visible: false,
});

layers['baselayer'] = new ol.layer.Tile({
    title: 'Base map',
    type: 'Base',
    source: new ol.source.TileImage({
        projection: 'EPSG:4326',
        tileGrid: tileGrid,
        url: 'https://tile.gbif.org/4326/omt/{z}/{x}/{y}@{r}x.png?style=gbif-middle'.replace('{r}', pixel_ratio),
        tilePixelRatio: pixel_ratio,
        wrapX: false
    }),
});

layers['baselayer-labels'] = new ol.layer.Tile({
    title: 'Base map (labelled)',
    type: 'Base',
    source: new ol.source.TileImage({
        projection: 'EPSG:4326',
        tileGrid: tileGrid,
        url: 'https://tile.gbif.org/4326/omt/{z}/{x}/{y}@{r}x.png?style=gbif-natural-en'.replace('{r}', pixel_ratio),
        tilePixelRatio: pixel_ratio,
        wrapX: false
    }),
    visible: false
});

layers['baselayer-osm'] = new ol.layer.Tile({
    title: 'OpenStreetMap',
    type: 'Base',
    source: new ol.source.OSM({
        wrapX: false
    }),
    visible: false
});

layers['bitmapCache'] = new ol.layer.Tile({
    title: 'Bitmap cache',
    source: new ol.source.TileImage({
        projection: 'EPSG:4326',
        tileGrid: tileGridBitmapCache,
        url: urlBase + '/bitmap',
        tilePixelRatio: 1,
        wrapX: false
    }),
    opacity: 0.5,
    visible: false
});

layers['political'] = new ol.layer.VectorTile({
    title: 'Political',
    renderMode: 'image',
        source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/political/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: true
});

layers['continent'] = new ol.layer.VectorTile({
    title: 'Continent',
    renderMode: 'image',
        source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/continent/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['gadm5'] = new ol.layer.VectorTile({
    title: 'GADM5',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/gadm5/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['gadm4'] = new ol.layer.VectorTile({
    title: 'GADM4',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/gadm4/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['gadm3'] = new ol.layer.VectorTile({
    title: 'GADM3',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/gadm3/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['gadm2'] = new ol.layer.VectorTile({
    title: 'GADM2',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/gadm2/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['gadm1'] = new ol.layer.VectorTile({
    title: 'GADM1',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/gadm1/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['iho'] = new ol.layer.VectorTile({
    title: 'IHO',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/iho/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false,
});

layers['wgsrpd'] = new ol.layer.VectorTile({
    title: 'WGSRPD',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/wgsrpd/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['centroids'] = new ol.layer.VectorTile({
    title: 'Centroids',
    renderMode: 'image',
    source: new ol.source.VectorTile({
        projection: 'EPSG:4326',
        format: new ol.format.MVT(),
        tileGrid: tileGrid,
        tilePixelRatio: 8,
        url: urlBase + '/tile/centroids/{z}/{x}/{y}.mvt',
        wrapX: false,
    }),
    style: countryStyle(),
    visible: false
});

layers['occurrences_density'] = new ol.layer.Tile({
    title: 'Density',
    source: new ol.source.TileImage({
        projection: 'EPSG:4326',
        tileGrid: tileGrid,
        url: occurrences_density_url,
        tilePixelRatio: pixel_ratio,
        wrapX: false
    }),
    visible: false
});

layers['occurrences_adhoc'] = new ol.layer.Tile({
    title: 'Ad-hoc',
    source: new ol.source.TileImage({
        projection: 'EPSG:4326',
        tileGrid: tileGrid,
        url: occurrences_adhoc_url,
        tilePixelRatio: pixel_ratio,
        wrapX: false
    }),
    visible: false
});

var source = new ol.source.Vector({
    projection: 'EPSG:4326',
});
var vector = new ol.layer.Vector({
    source: source,
    style: new ol.style.Style({
        fill: new ol.style.Fill({
            color: 'rgba(255, 255, 255, 0.8)'
        }),
        stroke: new ol.style.Stroke({
            color: '#ffcc33',
            width: 0.5
        }),
    })
});

var map = new ol.Map({
    layers: [
        layers['bitmapCache'],
        new ol.layer.Group({
            title: 'Base',
            layers: [
                layers['baselayer-osm'],
                layers['baselayer'],
                layers['baselayer-labels']
            ]
        }),
        new ol.layer.Group({
            title: 'Occurrences',
            layers: [
                layers['occurrences_adhoc'],
                layers['occurrences_density']
            ]
        }),
        new ol.layer.Group({
            title: 'Layer',
            layers: [
                layers['centroids'],
                layers['wgsrpd'],
                layers['iho'],
                layers['gadm5'],
                layers['gadm4'],
                layers['gadm3'],
                layers['gadm2'],
                layers['gadm1'],
                layers['continent'],
                layers['political']
            ]
        }),
        vector,
        layers['grid']
    ],
    target: 'map',
    view: view,
    controls: ol.control.defaults({
        attributionOptions: ({
            collapsible: false
        })
    }).extend([
        new ol.control.ScaleLine()
    ])
});


// Geocode — code copied from OpenLayers example.
var container = document.getElementById('popup');
var content = document.getElementById('popup-content');
var closer = document.getElementById('popup-closer');

/**
 * Create an overlay to anchor the popup to the map.
 */
var overlay = new ol.Overlay(({
    element: container,
    autoPan: true,
    autoPanAnimation: {
        duration: 250
    }
}));
map.addOverlay(overlay);

/**
 * Add a click handler to hide the popup.
 * @return {boolean} Don't follow the href.
 */
closer.onclick = function() {
    overlay.setPosition(undefined);
    closer.blur();
    return false;
};

/**
 * Add a click handler to the map to render the popup.
 */
function updateUncertaintyFromMeters() {
    uncertaintyDegrees_input.value = uncertaintyMeters_input.value / (111319.491 * Math.cos(latitude_input.value * Math.PI / 180));
    updateUncertainty = updateUncertaintyFromMeters;
}
function updateUncertaintyFromDegrees() {
    uncertaintyMeters_input.value = uncertaintyDegrees_input.value * (111319.491 * Math.cos(latitude_input.value * Math.PI / 180));
    updateUncertainty = updateUncertaintyFromDegrees;
}
updateUncertainty = updateUncertaintyFromDegrees;

function geocode(coordinate) {
  updateUncertainty();
  var uncertainty = parseFloat(uncertaintyDegrees_input.value);
    var radius = 110698.10348827201*uncertainty
    var circle = ol.geom.Polygon.circular(coordinate, radius, 32);
    source.addFeature(new ol.Feature(circle));

    var template = 'lat={y}&lng={x}';
    var url = urlBase + "/reverse?"+ol.coordinate.format(coordinate, template, 5)+"&uncertaintyDegrees="+uncertaintyDegrees_input.value;
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var response = JSON.parse(this.response);
            var text = response.map(function(x) { return '<tr><td>'+Number(x.distance).toFixed(3)+'</td><td>'+x.type+'</td><td><code>'+x.isoCountryCode2Digit+'</code></td><td><code>'+x.id+'</code></td><td>'+x.title+'</td></tr>'; }).join('');
            var template = '{y}N,{x}E';
            content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code> ' +
                    '<a href="' + url + '">⭞</a>' +
                    '<table><tr><th>Dist</th><th>Type</th><th>ISO</th><th>Identifier</th><th>Title</th></tr>' + text + '</table>';
        }
    };
    console.log(url);
    xhttp.open("GET", url, true);
    xhttp.send();

    var template = '{y}N,{x}E';
    content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code><ul><li>Loading</li></ul>';
    overlay.setPosition(coordinate);
}
map.on('singleclick', function(evt) {
    var coordinate = evt.coordinate;
    longitude_input.value = coordinate[0];
    latitude_input.value = coordinate[1];
    geocode(coordinate);
});

var layerSwitcher = new ol.control.LayerSwitcher({
    activationMode: 'click',
    startActive: true,
    tipLabel: 'Layers',
    groupSelectStyle: 'children'
});
map.addControl(layerSwitcher);

var latitude_input = document.getElementById('latitude_input');
latitude_input.onchange = (function(e) {
    updateUncertainty();
    var coordinate = [parseFloat(longitude_input.value), parseFloat(latitude_input.value)];
    geocode(coordinate);
});

var longitude_input = document.getElementById('longitude_input');
longitude_input.onchange = (function(e) {
    var coordinate = [parseFloat(longitude_input.value), parseFloat(latitude_input.value)];
    geocode(coordinate);
});

document.getElementById('uncertaintyMeters_input').onchange = updateUncertaintyFromMeters;
document.getElementById('uncertaintyDegrees_input').onchange = updateUncertaintyFromDegrees;

var occurrences_density_input = document.getElementById('occurrences_density_input');
occurrences_density_input.onchange = (function(e) {
    layers['occurrences_density'].setSource(
        new ol.source.TileImage({
            projection: 'EPSG:4326',
            tileGrid: tileGrid,
            url: occurrences_density_url+'&'+occurrences_density_input.value,
            tilePixelRatio: pixel_ratio,
            wrapX: false
        }));
});
occurrences_density_input.onchange();

var occurrences_adhoc_input = document.getElementById('occurrences_adhoc_input');
occurrences_adhoc_input.onchange = (function(e) {
    layers['occurrences_adhoc'].setSource(
        new ol.source.TileImage({
            projection: 'EPSG:4326',
            tileGrid: tileGrid,
            url: occurrences_adhoc_url+'&'+occurrences_adhoc_input.value,
            tilePixelRatio: pixel_ratio,
            wrapX: false
        }));
});
occurrences_adhoc_input.onchange();
