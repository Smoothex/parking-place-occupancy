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
    // change of polygon selection
    /*if(this.activePolygonInteractionLayer != e){
      console.log("clicked next polygon")
      this.activePolygonInteractionLayer.target.editing.disable();
      this.activePolygonInteractionLayer = e
    }*/
    console.log("Map interaction ", this.activePolygonInteractionLayer)
    if (this.activePolygonInteractionLayer != undefined) {
      console.log('New edited polygon', this.activePolygonInteractionLayer);
      const polygonID = this.activePolygonInteractionLayer.sourceTarget._leaflet_id;
      const index = this.parkingSpaceData.findIndex(
        (item: any) => item.polygon_layer._leaflet_id === polygonID
      );
      const oldGeometry =this.removeDuplicates(this.parkingSpaceData[index].simplified_edited_coordinates)
      var newDuplGeomerty = this.activePolygonInteractionLayer.sourceTarget._latlngs[0].map((item: any) => {
        return [item.lat, item.lng];
      });
      const newGeomerty = this.removeDuplicates(newDuplGeomerty)
      console.log("old geometry",oldGeometry)
      console.log("new geometry",newGeomerty)
      // TODO: add if any coordinate changed only show alert otherwise leave it be
      const sameOrNot=this.arraysHaveSameElementsUpToLastSecond(newGeomerty , oldGeometry)
      console.log("same or not ", sameOrNot)

      if(!sameOrNot){
      this.activePolygonInteractionLayer.target.editing.disable();
      alert('The last edited polygon will be edited permanently');
      this.editPolygonEvent(this.activePolygonInteractionLayer);
      }else{
      this.activePolygonInteractionLayer.target.editing.disable();
      }

      console.log('editing disabled');
      this.activePolygonInteractionLayer = undefined;
      // TODO: add editing check here
    }
  }

  constructor(
    private restApi: RestAPIService,
    private storage: PlayStorageService,
    private router: Router
  ) {}

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
    this.restApi.getAllParkingSpaces().then(
      (data: any) => {
        data.forEach((element: any) => {
          // const parseElement = JSON.parse(element);
          const parseElement = element;
          // console.log(element);
          this.restApi
            .getParkingSpaceAreaWithId(parseElement.id)
            .then(
              (areaData) => {
                return areaData;
              },
              (error) => {
                console.error('error getParkingSpaceAreawihID' + error);
              }
            )
            .then((area) => {
              let coordinatesArray = parseElement.coordinates;
              const arrayOfArrays = coordinatesArray.map((obj: any) => [
                obj.y,
                obj.x,
              ]);
              // console.log(arrayOfArrays);
              parseElement.coordinates = arrayOfArrays;
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
                    'Capacity: ' +
                    parseElement.capacity +
                    '<br>' +
                  '<div style="text-align: center; margin-top: 10px;">To edit, drag the markers on the border, and click outside the parking place to save it</div>'
                    // ' <button (click)="`${enable}`">Edit</button>'
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
                // console.log("before ", this.activePolygonInteractionLayer.toString())
                if (this.activePolygonInteractionLayer != undefined) {
                  this.activePolygonInteractionLayer.target.editing.disable()
                  this.mapClickInteraction(event);
                }
                if(this.activePolygonInteractionLayer === undefined){

                  this.activePolygonInteractionLayer = event;
                }
                console.log(event + 'parse' + parseElement.id);
                polygon.bringToFront();

                console.log(event);
                event.target.editing.enable();
                // TODO: add timestamp component here
                // this.restApi.getTimestampData(p)
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
        this.storage.storeData(this.parkingSpaceData);
        setTimeout(() => {
          // this.router.navigate(['/play'])
        }, 500);
      },
      (error) => {
        console.error('get all parking space data', error);
      }
    );
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

    this.restApi
      .updateParkingSpaceWithId(
        this.parkingSpaceData[index].parkingSpaceId,
        newGeometry
      )
      .then((result) => {
        this.parkingSpaceData[index].simplified_edited_coordinates =
          newGeometry.map((item: any) => {
            return [item[1], item[0]];
          });
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

  arraysHaveSameElementsUpToLastSecond(arr1: any[], arr2: any[]): boolean {
    const stringifyCoordinates = (arr: number[][]) => arr.map(coord => coord.join(','));

    const stringifiedArr1 = stringifyCoordinates(arr1);
    const stringifiedArr2 = stringifyCoordinates(arr2);

    return (
      stringifiedArr1.length === stringifiedArr2.length &&
      new Set(stringifiedArr1).size === new Set(stringifiedArr2).size &&
      stringifiedArr1.every(element => new Set(stringifiedArr2).has(element))
    );
  }

  removeDuplicates(coordinates: any[][]): any[] {
    const uniqueCoordinates = coordinates.filter(
      (coord, index, self) =>
        index ===
        self.findIndex(
          (c) => c[0] === coord[0] && c[1] === coord[1]
        )
    );

    return uniqueCoordinates;
  }
}
