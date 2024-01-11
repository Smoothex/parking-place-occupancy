import { Component } from '@angular/core';
import * as L from 'leaflet';
import { RestAPIService } from 'src/app/services/restAPI/rest-api.service';
import 'leaflet-draw';
import * as turf from '@turf/turf';
import { PlayStorageService } from 'src/app/services/playStorage/play-storage.service';
import { Router } from '@angular/router';
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

  public debug: boolean = false;

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

  /*
   * @deprecated will not be used anymore
   * */

  polygonColor: string = 'green';

  private initMap(): void {
    this.map = L.map('map').setView([52.54412, 13.352], 15);
    var satelliteLayer = L.tileLayer(
      'https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}',
      {
        maxZoom: 24,
        subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
        attribution: 'Google Satellite',
      }
    );

    var defaultLayer = L.tileLayer(
      'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
      {
        maxZoom: 24,
        attribution: 'OpenStreetMap',
      }
    );

    // Add the default layer to the map
    defaultLayer.addTo(this.map);

    // Create an object with layer names and corresponding layers
    var baseLayers = {
      Satellite: satelliteLayer,
      Default: defaultLayer,
    };

    // Add layer control to switch between different layers
    L.control.layers(baseLayers).addTo(this.map);
    this.map.on('click', this.mapClickInteraction.bind(this));
  }

  mapClickInteraction(e: any) {
    console.log('clicked on map', e.latlng);
    if (this.debug) {
      console.log('Markers present before:', this.markerLayer);
    }
    if (this.activePolygonInteractionLayer != undefined) {
      console.log('New edited polygon', this.activePolygonInteractionLayer);
      alert('The last edited polygon will be edited permanently');
      this.editPolygonEvent(this.activePolygonInteractionLayer);
      this.activePolygonInteractionLayer.target.editing.disable();
      console.log('editing disabled');
      this.activePolygonInteractionLayer = undefined;
      // TODO: add editing check here
    }
  }

  constructor(private restApi: RestAPIService, private storage:PlayStorageService, private router: Router) {}

  ngAfterViewInit(): void {
    this.initMap();
    setTimeout(() => {
      this.editableLayer();
      this.markParkingPlaces();
    }, 500);
  }
  highlightFeature(e: any) {
    let layer = e.target;

    layer.setStyle({
      fillColor: 'blue', // Change this to the color you want on mouseover
      weight: 2,
      opacity: 1,
    });
  }

  // Function to reset polygon color on mouseout
  resetFeature(e: any) {
    let layer = e.target;
    let color = layer.options.color;

    layer.setStyle({
      fillColor: color,
      color: color, // Change this to the original color
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
              return areaData;
            },
            (error) => {}
          )
          .then((area) => {
            let coordinatesArray = parseElement.polygon.coordinates;
            const arrayOfArrays = coordinatesArray.map((obj: any) => [
              obj.y,
              obj.x,
            ]);
            parseElement.polygon.coordinates = arrayOfArrays;
            const simple = this.simplifyGeoPolygon(arrayOfArrays);
            let polygon = L.polygon(simple)
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
            let polygonColor: string = 'green';
            if (parseElement.occupied) {
              polygonColor = 'red';
            }
            polygon.setStyle({
              fillColor: polygonColor,
              color: polygonColor,
              opacity: 1,
              weight: 3,
            });
            polygon.on({
              mouseover: (event: any) => {
                this.highlightFeature(event);
              },
              mouseout: (event: any) => {
                this.resetFeature(event);
              },
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
              new_polygon_event_layer: polygon,
              simplified_initial_coordinates: simple,
              simplified_edited_coordinates: simple, // will be updated with every update in polygon in one user session
            };
            this.parkingSpaceData.push(obj);

          });
      });
      console.log('parking space data ', this.parkingSpaceData);
      this.storage.storeData(this.parkingSpaceData)
      setTimeout(() => {
        
        // this.router.navigate(['/play'])
      }, 500);

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
    let marker = L.marker(latlng, markerIcon).addTo(this.map);
    marker.on('drag', function (e: any) {
      console.log('dragging marker');
      console.log('marker position ', e.latlng);
    });
    this.markerLayer.push(marker);
    return marker;
  }
  /**
   * This method functionality is only for debugging purposes, which uses an external library to create and draw the polygon
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

  editPolygonEvent(polygonEvent: any) {
    /*
     * TODO: remove markers being placed on the same spot ~ done
     * TODO: find the coordinate in the real polygon and edit/replace them
     ! * TODO: update the data value array ~ Not implemented, have to do think about the redo feature ~ not a necessary function 
     * TODO: re-create only the particular array~ no need as the old polygon shape can not be agin defined
     * TODO: add checks for not creating an impossible polygon
     */
    const polygonID = polygonEvent.sourceTarget._leaflet_id;
    const index = this.parkingSpaceData.findIndex(
      (item: any) => item.polygon_layer._leaflet_id === polygonID
    );
    this.parkingSpaceData[index].new_polygon_event_layer = polygonEvent;
    // ! TODO : remove when backend capable to do it
    this.parkingSpaceData[index].simplified_edited_coordinates =
      this.parkingSpaceData[
        index
      ].new_polygon_event_layer.sourceTarget._latlngs[0];

    console.log(this.parkingSpaceData[index]);
    const newGeometry: any[] = this.parkingSpaceData[
      index
    ].new_polygon_event_layer.sourceTarget._latlngs[0].map((item: any) => {
      return [item.lng, item.lat];
    });

    //TODO: REST POST call to save it in the database. ~ done
    this.restApi
      .updateParkingSpaceWithId(
        this.parkingSpaceData[index].parkingSpaceId,
        newGeometry
      )
      .then((result) => {
       this.parkingSpaceData[index].simplified_edited_coordinates =newGeometry.map((item: any) => {
          return [item[1],item[0]]
        })
      });
  }
  // future feature request
  editableLayer() {
    let editableLayers = new L.FeatureGroup();
    this.map.addLayer(editableLayers);

    let MyCustomMarker = L.Icon.extend({
      options: {
        shadowUrl: null,
        iconAnchor: new L.Point(12, 12),
        iconSize: new L.Point(24, 24),
        iconUrl: '../../assets/marker-icon.png',
      },
    });
    let options: any = {
      position: 'topright',
      draw: {
        polyline: false,
        polygon: false /* {
          allowIntersection: false, // Restricts shapes to simple polygons
          drawError: {
            color: '#e1e100', // Color the shape will turn when intersects
            message: "<strong>Oh snap!<strong> you can't draw that!", // Message that will show when intersect
          },
          shapeOptions: {
            color: 'blue',
          },
        } */,
        marker: false /* {
          icon: new MyCustomMarker(),
        }, */,
        circle: false,
        rectangle: false,
        circlemarker: false,
      },
      edit: false /* {
        featureGroup: editableLayers, //REQUIRED!!
        remove: false,
      } */,
    };
    let drawControl = new L.Control.Draw(options);
    this.map.addControl(drawControl);

    this.map.on(L.Draw.Event.CREATED, function (e: any) {
      let type = e.layerType,
        layer = e.layer;

      if (type === 'marker') {
        layer.bindPopup('A popup!');
      }

      editableLayers.addLayer(layer);
    });
  }
}
