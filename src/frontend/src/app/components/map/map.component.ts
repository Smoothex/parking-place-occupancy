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
  /**
   * Centralized local data storage which will help in keeping updated and edited polygons 
   * ! Do not delete or change any line of code if it is implemented in the code
   */
  private parkingSpaceData: any = [];
  /**
   * !This value should not be changed as it has been optimized for showing better polygons with less coordinates
   */
  public tolerance = 0.0000005
  public debug:boolean = true;

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
    if (this.debug) {
      console.log("Markers present before:", this.markerLayer)
    }
    if (this.markerLayer) {
      this.markerLayer.forEach((singleMarker: L.Marker) => {
        this.map.removeLayer(singleMarker) 
      });
      setTimeout(() => {
        this.markerLayer = []
        if (this.debug) {
          console.log("Markers present after:", this.markerLayer)
        }
      }, 300);

    }
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
              const simple = this.simplifyGeoPolygon(arrayOfArrays)
              simple.forEach((elem: any) => {
                const mark= this.createMarkerPersistentStorage(elem);
                
              });
              if (this.debug) {
                console.log("orignal", arrayOfArrays)
                this.addPolygon(arrayOfArrays)
                console.log("new", simple)
                console.log("added marker on the layer", this.markerLayer)
                
              }
              
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
    this.markerLayer.push(marker);
    return marker;
  }
/**
 * This method functionality is only for debigging purposes, which uses an external library to create and draw the polygon 
 * @param originalCoords coordinates for polygon
 */
  addPolygon(originalCoords:any[]) {
    // Simplify the polygon using turf.js
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = this.tolerance // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords:any = simplifiedGeoJSON.geometry.coordinates[0];
    if (this.debug) {
      const simplifiedPolygon = L.polygon(simplifiedCoords, { color: 'blue' }).addTo(this.map);
      console.log("Orignal array",originalCoords)
      console.log("New array",simplifiedCoords)
      
    }
  }
  simplifyGeoPolygon(originalCoords:any) {
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = this.tolerance; // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords:any = simplifiedGeoJSON.geometry.coordinates[0];
    return simplifiedCoords
  }
  // edit polygon 
  /**
   * TODO: remove markers being placed on the same spot 
   * TODO: find the coordinate in the real polygon and edit/replace them 
   * TODO: update the data value array
   * TODO: re-create only the particular array
   * TODO: add checks for not creating an impossible polygon 
   */


  editPolygon() {
    
  }
  



  // ! Not that important but might be usefull later on
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
