proj4.defs('EPSG:4326', "+proj=longlat +ellps=WGS84 +datum=WGS84 +units=degrees");

var resolutions = [];
var extent = 180.0;
var tile_size = 512;
var resolutions = Array(17).fill().map((_, i) => ( extent / tile_size / Math.pow(2, i) ));

var layers = [];

function countryColour(isocode) {
	var longer = (isocode+isocode+isocode+isocode+isocode+'');
	var hash = longer.hashCode();
	var r = (hash & 0xFF0000) >> 16;
	var g = (hash & 0x00FF00) >> 8;
	var b = hash & 0x0000FF;
	return "rgb(" + r + "," + g + "," + b + ")";
}

function makePattern(colour) {
	var cnv = document.createElement('canvas');
	var ctx = cnv.getContext('2d');
	cnv.width = 5;
	cnv.height = 5;
	ctx.fillStyle = colour;

	for(var i = 0; i < 5; ++i) {
		ctx.fillRect(i, i, 1, 1);
		ctx.fillRect(5-i, i, 1, 1);
	}

	return ctx.createPattern(cnv, 'repeat');
}

function countryStyle() {
	var point = new ol.style.Style({
		image: new ol.style.Circle({
			fill: new ol.style.Fill({color: '#FF0000'}),
			radius: 1
		}),
		fill: new ol.style.Fill({color: '#FF0000'})
	});

	var styles = [];
	return function(feature, resolution) {
		var length = 0;
		var isocode = feature.get('iso_a2');
		var colour = countryColour(isocode);

		styles[length++] = new ol.style.Style({
			fill: new ol.style.Fill({color: makePattern(colour)}),
			stroke: new ol.style.Stroke({color: colour})
		});
		styles.length = length;
		return styles;
	};
}

var tileGrid = new ol.tilegrid.TileGrid({
	extent: ol.proj.get('EPSG:4326').getExtent(),
	minZoom: 0,
	maxZoom: 16,
	resolutions: resolutions,
	tileSize: 512,
});

layers['baselayer'] = new ol.layer.Tile({
	source: new ol.source.TileImage({
	projection: 'EPSG:4326',
	tileGrid: tileGrid,
	url: 'https://tile.gbif.org/4326/omt/{z}/{x}/{y}@2x.png?style=osm-bright-en',
	tilePixelRatio: 2,
		wrapX: true
	}),
});

layers['geocode'] = new ol.layer.VectorTile({
	source: new ol.source.VectorTile({
		projection: 'EPSG:4326',
		format: new ol.format.MVT(),
		tileGrid: tileGrid,
		tilePixelRatio: 8,
		url: '/tiles/{z}/{x}/{y}.pbf',
		wrapX: false
	}),
	style: countryStyle(),
});

var map = new ol.Map({
	layers: [
		layers['baselayer'],
		layers['geocode']
	],
	target: 'map',
	view: new ol.View({
		center: [3.36722, 51.3625],
		projection: 'EPSG:4326',
		minZoom: 4,
		maxZoom: 16,
		zoom: 6
	}),
	controls: ol.control.defaults({
		attributionOptions: ({
			collapsible: false
		})
	}).extend([
		new ol.control.ScaleLine()
	])
});

map.addOverlay(new ol.Overlay({
	position: [3.36722, 51.3625],
	positioning: 'bottom-center',
	element: document.getElementById('icon'),
}));

map.addOverlay(new ol.Overlay({
	position: [3.36722, 51.3625],
	positioning: 'top-center',
	element: document.getElementById('icon2'),
}));

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
	var url = "http://localhost:8080/geocode/reverse?"+ol.coordinate.format(coordinate, template, 5);
	console.log(url);
	xhttp.open("GET", url, true);
	xhttp.send();

	var url = "http://api.gbif.org/v1/geocode/reverse?"+ol.coordinate.format(coordinate, template, 5);
	console.log(url);

	var template = '{y}N,{x}E';
	content.innerHTML = '<code>' + ol.coordinate.format(coordinate, template, 5) + '</code><ul><li>Loading</li></ul>';
	overlay.setPosition(coordinate);
});
