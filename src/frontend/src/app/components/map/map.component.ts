import { Component } from '@angular/core';
import * as L from 'leaflet';
import { RestAPIService } from 'src/app/services/restAPI/rest-api.service';
import 'leaflet-draw';
import * as turf from '@turf/turf';
@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css'],
})
export class MapComponent {
  private map: any;
  private markerLayer: any = [];
  private parkingSpaceData: any = [];
  public tolerance = 0.00000005

  markerIcon = {
    draggable: true,
    icon: L.icon({
      iconSize: [25, 41],
      iconAnchor: [12.5, 41],
      // specify the path here
      iconUrl: '../../assets/marker-icon.png',
      shadowUrl: '../../assets/marker-shadow.png',
    }),
  };

  RedmarkerIcon = {
    draggable: true,
    icon: L.icon({
      iconSize: [25, 41],
      iconAnchor: [12.5, 41],
      // specify the path here
      iconUrl: '../../assets/red.png',
    }),
  };
  polygonColor: string = 'green';

  private initMap(): void {
    this.map = L.map('map').setView([52.54412, 13.352], 15);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 24,
      attribution:
        '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.map);

    this.map.on('click', this.giveCoordinate.bind(this));
  }

  giveCoordinate(e: any) {
    console.log('clicked on map', e.latlng);
    // var marker = L.marker(e.latlng, this.icon)
    //   marker.addTo(this.map)
    // todo: any markers present remove it automatically
    this.map.removeLayer(this.markerLayer);
  }

  constructor(private restApi: RestAPIService) {}

  ngAfterViewInit(): void {
    this.initMap();
    setTimeout(() => {
      this.editableLayer();
      this.markParkingPlaces();
    }, 500);
  }

  markParkingPlaces(): void {
    this.restApi.getAllParkingSpaces().then((data: any) => {
      data.forEach((element: any) => {
        const parseElement = JSON.parse(element);
        this.restApi
          .getParkingSpaceAreaWithId(parseElement.id)
          .then(
            (areaData) => {
              // console.log(areaData)
              return areaData;
            },
            (error) => {}
          )
          .then((area) => {
            var coordinatesArray = parseElement.polygon.coordinates;
            const arrayOfArrays = coordinatesArray.map((obj: any) => [
              obj.y,
              obj.x,
            ]);
            parseElement.polygon.coordinates = arrayOfArrays;
            let obj = {
              ...parseElement,
              // "marker":arrayOfArrays
            };
            this.parkingSpaceData.push(obj);

            var polygon = L.polygon(arrayOfArrays)
              .addTo(this.map)
              .bindPopup(
                'Id: ' +
                  parseElement.id +
                  '<br>' +
                  'Occupied: ' +
                  JSON.stringify(parseElement.occupied) +
                  ' <br>' +
                  'Area: ' +
                  area +
                  ' m&sup2;' +
                  '<br>' +
                  ' <button (click)="">Edit</button>'
              );
            if (parseElement.occupied == true) {
              this.polygonColor = 'red';
            }
            polygon.setStyle({
              fillColor: this.polygonColor,
              color: this.polygonColor,
            });
            polygon.on('click', (event) => {
              console.log(event + 'parse' + parseElement.id);
              polygon.bringToFront();
              console.log("orignal", arrayOfArrays)
              const simple = this.simplifyGeoPolygon(arrayOfArrays)
              console.log("new", simple)
              console.log("starting minimal version of markers ")
              this.addPolygon(arrayOfArrays)
              arrayOfArrays.forEach((elem: any) => {
                const marker = this.createMarkerPersistentStorage(elem);
                //! add markers to the parking space data make sure of the order of adding marker
                // console.log(polygon);
              });
              simple.forEach((elem: any) => {
                const mark= this.createMarkerPersistentStorage(elem, this.RedmarkerIcon);
                
              });
              
            });
          });
      });
      console.log(this.parkingSpaceData);
    });
  }
  /**
   * Event handler activated while changing the polygon shape
   * @param latlng geolocation of marker on the map
   */
  createMarkerPersistentStorage(latlng: L.LatLng,markerIcon: any = this.markerIcon): L.Marker {
    var marker = L.marker(latlng, markerIcon).addTo(this.map);
    marker.on('drag', function (e: any) {
      console.log('dragging marker');
      console.log('marker position ', e.latlng);
    });
    return marker;
    // this.markerLayer.push(marker);
  }

  addPolygon(originalCoords:any[]) {
  


    // Simplify the polygon using turf.js
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = this.tolerance // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords:any = simplifiedGeoJSON.geometry.coordinates[0];

    const simplifiedPolygon = L.polygon(simplifiedCoords, { color: 'blue' }).addTo(this.map);
    console.log("Orignal array",originalCoords)
    console.log("New array",simplifiedCoords)
  }
  simplifyGeoPolygon(originalCoords:any) {
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = this.tolerance; // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords:any = simplifiedGeoJSON.geometry.coordinates[0];
    return simplifiedCoords
  }
  editableLayer() {
    var editableLayers = new L.FeatureGroup();
    this.map.addLayer(editableLayers);

    var MyCustomMarker = L.Icon.extend({
      options: {
        shadowUrl: null,
        iconAnchor: new L.Point(12, 12),
        iconSize: new L.Point(24, 24),
        iconUrl: '../../assets/marker-icon.png',
      },
    });
    var options: any = {
      position: 'topright',
      draw: {
        polyline: false,
        polygon: {
          allowIntersection: false, // Restricts shapes to simple polygons
          drawError: {
            color: '#e1e100', // Color the shape will turn when intersects
            message: "<strong>Oh snap!<strong> you can't draw that!", // Message that will show when intersect
          },
          shapeOptions: {
            color: '#bada55',
          },
        },
        marker: {
          icon: new MyCustomMarker()
      },
        circle: false, // Turns off this drawing tool
        rectangle: false,
        circlemarker: false,
      },
      edit: {
        featureGroup: editableLayers, //REQUIRED!!
        remove: false,
      },
    };
    var drawControl = new L.Control.Draw(options);
    this.map.addControl(drawControl);

    this.map.on(L.Draw.Event.CREATED, function (e: any) {
      var type = e.layerType,
        layer = e.layer;

      if (type === 'marker') {
        layer.bindPopup('A popup!');
      }

      editableLayers.addLayer(layer);
    });
  }
}
