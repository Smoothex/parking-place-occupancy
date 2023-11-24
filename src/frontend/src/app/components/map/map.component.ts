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
  /**
   * consist of all the markers present in current layer of the map
   */
  private markerLayer: any = [];
  private activePolygonInteractionLayer: any = undefined;

  /**
   * Centralized local data storage which will help in keeping track of updated and edited polygons
   * ! Do not delete or change any line of code if it is implemented in the code
   */
  private parkingSpaceData: any = [];
  /**
   * !This value should not be changed as it has been optimized for showing better polygons with less coordinates
   */
  public tolerance = 0.0000005;

  public debug: boolean = true;

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

    this.map.on('click', this.mapClickInteraction.bind(this));
  }

  mapClickInteraction(e: any) {
    console.log('clicked on map', e.latlng);
    if (this.debug) {
      console.log('Markers present before:', this.markerLayer);
    }
    if (this.markerLayer) {
      this.markerLayer.forEach((singleMarker: L.Marker) => {
        this.map.removeLayer(singleMarker);
      });
      setTimeout(() => {
        this.markerLayer = [];
        if (this.debug) {
          console.log('Markers present after:', this.markerLayer);
        }
      }, 300);
    }
    if (this.activePolygonInteractionLayer != undefined) {
      console.log(this.activePolygonInteractionLayer);
      //TODO: another of clause saying changes will be saved or deleted after clicking ok
      alert('The last edited polygon will be edited permanently');
      this.editPolygon(this.activePolygonInteractionLayer);
      this.activePolygonInteractionLayer.target.editing.disable();
      this.activePolygonInteractionLayer = undefined;
      // TODO: add editing check here
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
  highlightFeature(e: any) {
    var layer = e.target;

    layer.setStyle({
      fillColor: 'blue', // Change this to the color you want on mouseover
      weight: 2,
      opacity: 1,
    });
  }

  // Function to reset polygon color on mouseout
  resetFeature(e: any) {
    var layer = e.target;

    layer.setStyle({
      fillColor: 'green',
      color: 'green', // Change this to the original color
      weight: 2,
      opacity: 1,
    });
  }

  // Event listeners for mouseover and mouseout

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

            // TODO: add another property for all the polygon
            // TODO: polygon.sourceTarget leafert _id will be used for mapping
            const simple = this.simplifyGeoPolygon(arrayOfArrays);
            var polygon = L.polygon(simple)
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
                  ' <button (click)="`${enable}`">Edit</button>'
              );
            if (parseElement.occupied == true) {
              this.polygonColor = 'red';
            }
            polygon.setStyle({
              fillColor: this.polygonColor,
              color: this.polygonColor,
            });
            polygon.on({
              mouseover: this.highlightFeature,
              mouseout: this.resetFeature,
            });
            polygon.on('click', (event) => {
              if (this.activePolygonInteractionLayer != undefined) {
                this.mapClickInteraction(event);
              } else {
                this.activePolygonInteractionLayer = event;
              }
              console.log(event + 'parse' + parseElement.id);
              polygon.bringToFront();
              console.log(event);

              event.target.editing.enable();

              const simple = this.simplifyGeoPolygon(arrayOfArrays);
              // simple.forEach((elem: any) => {
              //   const mark = this.createMarkerPersistentStorage(elem);
              // });
              if (this.debug) {
                console.log('orignal', arrayOfArrays);
                this.addPolygon(arrayOfArrays);
                console.log('new', simple);
                console.log('added marker on the layer', this.markerLayer);
              }
            });
            // adding historical data for the polygons
            // properties which could be edited would be stored here
            let obj = {
              ...parseElement,
              polygon_layer: polygon,
              new_polygon_layer: polygon,
            };
            this.parkingSpaceData.push(obj);
          });
      });
      console.log('parking space data ', this.parkingSpaceData);
    });
  }
  /**
   * Event handler activated while changing the polygon shape
   * @param latlng geolocation of marker on the map
   */
  createMarkerPersistentStorage(
    latlng: L.LatLng,
    markerIcon: any = this.markerIcon
  ): L.Marker {
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
  addPolygon(originalCoords: any[]) {
    // Simplify the polygon using turf.js
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = this.tolerance; // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords: any = simplifiedGeoJSON.geometry.coordinates[0];
    if (this.debug) {
      const simplifiedPolygon = L.polygon(simplifiedCoords, {
        color: 'blue',
      }).addTo(this.map);
      console.log('Orignal array', originalCoords);
      console.log('New array', simplifiedCoords);
    }
  }
  simplifyGeoPolygon(originalCoords: any) {
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = this.tolerance; // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords: any = simplifiedGeoJSON.geometry.coordinates[0];
    return simplifiedCoords;
  }

  editPolygon(polygonEvent: any) {
    /*
     * TODO: remove markers being placed on the same spot
     * TODO: find the coordinate in the real polygon and edit/replace them
     * TODO: update the data value array
     * TODO: re-create only the particular array
     * TODO: add checks for not creating an impossible polygon
     */
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
            color: 'blue',
          },
        },
        marker: {
          icon: new MyCustomMarker(),
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
