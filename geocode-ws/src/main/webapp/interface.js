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
var resolutions = Array(13).fill().map((_, i) => ( extent / tile_size / Math.pow(2, i) ));

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
		var isocode = feature.get('id');
		var colour = countryColour(isocode);
		var invertColour = oppositeCountryColour(isocode);

		if (feature.get('type') == 'Political') {
			pattern = makePatternB(colour);
		} else if (feature.get('type') == 'EEZ') {
			pattern = makePatternH(colour);
		} else {
			pattern = makePatternF(colour);
		}

		if (feature.getType() == 'Point') {
			var featureFill = new ol.style.Fill({color: colour});

			text.getText().setFill(featureFill);
			if (window.location.hash !== '') {
				text.getText().setText(stringDivider(feature.get('title'), 16, '\n'));
				text.getText().getStroke().setColor(invertColour);
				styles[length++] = text;
			}

			styles[length++] = new ol.style.Style({
				image: new ol.style.Circle({
					fill: text.getText().getFill(),
					stroke: new ol.style.Stroke({color: invertColour, width: 1}),
					radius: 8
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

layers['baselayer'] = new ol.layer.Tile({
	title: 'Base map',
	type: 'Base',
	source: new ol.source.TileImage({
		projection: 'EPSG:4326',
		tileGrid: tileGrid,
		url: 'https://tile.gbif.org/4326/omt/{z}/{x}/{y}@2x.png?style=gbif-light',
		tilePixelRatio: 2,
		wrapX: false
	}),
});

layers['baselayer-labels'] = new ol.layer.Tile({
	title: 'Base map (labelled)',
	type: 'Base',
	source: new ol.source.TileImage({
		projection: 'EPSG:4326',
		tileGrid: tileGrid,
		url: 'https://tile.gbif.org/4326/omt/{z}/{x}/{y}@2x.png?style=gbif-geyser',
		tilePixelRatio: 2,
		wrapX: false
	}),
	visible: false
});

layers['bitmapCache'] = new ol.layer.Tile({
	title: 'Old bitmap cache',
	source: new ol.source.TileImage({
		projection: 'EPSG:4326',
		tileGrid: tileGridBitmapCache,
		url: './geocode/bitmap',
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
		url: './tile/political/{z}/{x}/{y}.mvt',
		wrapX: false,
	}),
	style: countryStyle(),
});

layers['eez'] = new ol.layer.VectorTile({
	title: 'EEZ',
	renderMode: 'image',
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGrid,
		tilePixelRatio: 8,
		url: './tile/eez/{z}/{x}/{y}.mvt',
		wrapX: false,
	}),
	style: countryStyle(),
	visible: false
});

layers['gadm'] = new ol.layer.VectorTile({
	title: 'GADM',
	renderMode: 'image',
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGrid,
		tilePixelRatio: 8,
		url: './tile/gadm/{z}/{x}/{y}.mvt',
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
		url: './tile/gadm5/{z}/{x}/{y}.mvt',
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
		url: './tile/gadm4/{z}/{x}/{y}.mvt',
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
		url: './tile/gadm3/{z}/{x}/{y}.mvt',
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
		url: './tile/gadm2/{z}/{x}/{y}.mvt',
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
		url: './tile/gadm1/{z}/{x}/{y}.mvt',
		wrapX: false,
	}),
	style: countryStyle(),
});

layers['gadm0'] = new ol.layer.VectorTile({
	title: 'GADM0',
	renderMode: 'image',
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGrid,
		tilePixelRatio: 8,
		url: './tile/gadm0/{z}/{x}/{y}.mvt',
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
		url: './tile/iho/{z}/{x}/{y}.mvt',
		wrapX: false,
	}),
	style: countryStyle(),
	visible: false,
});

layers['seavox'] = new ol.layer.VectorTile({
	title: 'SeaVoX',
	renderMode: 'image',
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGrid,
		tilePixelRatio: 8,
		url: './tile/seavox/{z}/{x}/{y}.mvt',
		wrapX: false,
	}),
	style: countryStyle(),
	visible: false
});

layers['geolocate_centroids'] = new ol.layer.VectorTile({
	title: 'Geolocate centroids',
	renderMode: 'image',
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGrid,
		tilePixelRatio: 8,
		url: './tile/geolocate_centroids/{z}/{x}/{y}.mvt',
		wrapX: false,
	}),
	style: countryStyle(),
});

var map = new ol.Map({
	layers: [
		layers['baselayer'],
		layers['baselayer-labels'],
		new ol.layer.Group({
			title: 'Layer',
			layers: [
				layers['political'],
				layers['eez'],
				layers['gadm'],
				layers['gadm5'],
				layers['gadm4'],
				layers['gadm3'],
				layers['gadm2'],
				layers['gadm1'],
				layers['gadm0'],
				layers['iho'],
				layers['seavox'],
				layers['geolocate_centroids'],
			]
		}),
		layers['bitmapCache'],
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

	var template = 'lat={y}&lng={x}';
	var url = "./geocode/reverse?"+ol.coordinate.format(coordinate, template, 5);
	var xhttp = new XMLHttpRequest();
	xhttp.onreadystatechange = function() {
		if (this.readyState == 4 && this.status == 200) {
			var response = JSON.parse(this.response);
			var text = response.map(function(x) { return '<li>'+x.title+' ('+x.type+', '+x.id+')</li>'; }).join('');
			var template = '{y}N,{x}E';
			content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code>' +
					'<ul>' + text + '</ul>' +
					'<a href="' + url + '">⭞</a>';
		}
	};
	console.log(url);
	xhttp.open("GET", url, true);
	xhttp.send();

	var template = '{y}N,{x}E';
	content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code><ul><li>Loading</li></ul>';
	overlay.setPosition(coordinate);
});

var layerSwitcher = new ol.control.LayerSwitcher();
map.addControl(layerSwitcher);
