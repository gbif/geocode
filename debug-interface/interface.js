proj4.defs('EPSG:4326', "+proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees");

var extent = 180.0;
var tile_size = 512;
/*
  Going by the tile schema used for base map tiles, we have geocoder tiles from zooms 2 and 6.
  At zoom 0, we show the zoom 2 tiles -- they will be 128px wide.

  0: 2@128
  1: 2@256
  2: 2@512
  3: 2@1024
  4: 2@2048
  5: 2@4096
  6: 6@512
  7: 6@1024
  8: 6@2048
  9: 6@4096

  We run out of detail at zoom 9, so this is the most detailed resolution.
*/
var resolutions = Array(10).fill().map((_, i) => ( extent / tile_size / Math.pow(2, i) ));

var tileGrid = new ol.tilegrid.TileGrid({
	extent: ol.proj.get('EPSG:4326').getExtent(),
	minZoom: 0,
	maxZoom: 16,
	resolutions: resolutions,
	tileSize: 512,
});

var tileGridZoom2Only = new ol.tilegrid.TileGrid({
	extent: ol.proj.get('EPSG:4326').getExtent(),
	minZoom: 0,
	maxZoom: 16,
	resolutions: [extent / tile_size / Math.pow(2, 2)],
	tileSize: 512,
});

var tileGridZoom6Only = new ol.tilegrid.TileGrid({
	extent: ol.proj.get('EPSG:4326').getExtent(),
	minZoom: 0,
	maxZoom: 16,
	resolutions: [extent / tile_size / Math.pow(2, 6)],
	tileSize: 512,
});

// This is for the bitmap cache, just use a single rectangulare 'tile'.
var tileGridBitmapCache = new ol.tilegrid.TileGrid({
	extent: ol.proj.get('EPSG:4326').getExtent(),
	minZoom: 0,
	maxZoom: 16,
	resolutions: [extent / 3600],
	tileSize: [7200, 3600],
});

var layers = [];

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

function makePatternF(colour) {
	var cnv = document.createElement('canvas');
	var ctx = cnv.getContext('2d');
	cnv.width = 4;
	cnv.height = 4;
	ctx.fillStyle = colour;

	for(var i = 0; i < cnv.width; i += 1) {
		ctx.fillRect(cnv.width-1-i, i, 1, 1);
	}

	return ctx.createPattern(cnv, 'repeat');
}

function makePatternB(colour) {
	var cnv = document.createElement('canvas');
	var ctx = cnv.getContext('2d');
	cnv.width = 10;
	cnv.height = 10;
	ctx.fillStyle = colour;

	for(var i = 0; i < 10; i += 2) {
		ctx.fillRect(i, i, 1, 1);
		ctx.fillRect(i+1, i+1, 1, 1);
		ctx.fillRect(i+3, i+3, 1, 1);
		ctx.fillRect(i+4, i+4, 4, 4);
	}

	return ctx.createPattern(cnv, 'repeat');
}

function countryStyle() {
	var styles = [];
	return function(feature, resolution) {
		var length = 0;
		var isocode = feature.get('iso_a2');
		var colour = countryColour(isocode);

		if (feature.get('type') == 'eez') {
			pattern = makePatternB(colour);
		} else {
			pattern = makePatternF(colour);
		}
		styles[length++] = new ol.style.Style({
			fill: new ol.style.Fill({color: pattern}),
			stroke: new ol.style.Stroke({color: colour})
		});
		styles.length = length;
		return styles;
	};
}

var view = new ol.View({
	center: [0, 20],
	projection: 'EPSG:4326',
	zoom: 2,
	resolutions: resolutions
});

layers['baselayer'] = new ol.layer.Tile({
	source: new ol.source.TileImage({
		projection: 'EPSG:4326',
		tileGrid: tileGrid,
		url: 'https://tile.gbif-uat.org/4326/omt/{z}/{x}/{y}@2x.png?style=osm-bright',
		tilePixelRatio: 2,
		wrapX: false
	}),
});

layers['bitmapCache'] = new ol.layer.Tile({
	source: new ol.source.TileImage({
		projection: 'EPSG:4326',
		tileGrid: tileGridBitmapCache,
		url: 'https://api.gbif.org/v1/geocode/bitmap',
		tilePixelRatio: 1,
		wrapX: false
	}),
	opacity: 0.5
});

layers['geocode'] = new ol.layer.VectorTile({
	renderMode: 'vector',
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGridZoom6Only,
		tilePixelRatio: 8,
		url: './tiles/6-{x}-{y}.pbf',
		wrapX: false,
	}),
	minResolution: view.getResolutionForZoom(16),
	maxResolution: view.getResolutionForZoom(5),
	style: countryStyle(),
});

layers['geocodeFast'] = new ol.layer.VectorTile({
	renderMode: 'image',
	source: new ol.source.VectorTile({
		attributions: ['<a href="./info.html">Help and information</a>'],
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGridZoom2Only,
		tilePixelRatio: 8,
		url: './tiles/2-{x}-{y}.pbf',
		wrapX: false,
	}),
	minResolution: view.getResolutionForZoom(5),
	style: countryStyle(),
});

var map = new ol.Map({
	layers: [
		layers['baselayer'],
		layers['bitmapCache'],
		layers['geocode'],
		layers['geocodeFast']
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
map.on('singleclick', function(evt) {
	var coordinate = evt.coordinate;

	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var response = JSON.parse(this.response);
			var text = response.map(function(x) { return '<li>'+x.title+' ('+x.type+', '+x.isoCountryCode2Digit+')</li>'; }).join('');
			var template = '{y}N,{x}E';
			content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code><ul>' + text + '</ul>';
		}
	};
	var template = 'lat={y}&lng={x}';
	var url = "http://api.gbif.org/v1/geocode/reverse?"+ol.coordinate.format(coordinate, template, 5);
	console.log(url);
	xhttp.open("GET", url, true);
	xhttp.send();

	var template = '{y}N,{x}E';
	content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code><ul><li>Loading</li></ul>';
	overlay.setPosition(coordinate);
});
