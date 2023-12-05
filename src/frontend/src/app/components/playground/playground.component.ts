import { Component } from '@angular/core';
import * as L from 'leaflet';
import { RestAPIService } from 'src/app/services/restAPI/rest-api.service';
import 'leaflet-draw';
import * as turf from '@turf/turf';

@Component({
  selector: 'app-playground',
  templateUrl: './playground.component.html',
  styleUrls: ['./playground.component.css']
})
export class PlaygroundComponent {

  private map: any;
  private markerLayer: any = [];
  private parkingSpaceData: any = [];
  editing:boolean = false;
  drawnItems:any

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


  private initMap(): void {
    this.map = L.map('map').setView(this.tryArray[0], 15);
    // L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    //   maxZoom: 24,
    //   attribution:
    //     '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    // }).addTo(this.map);
    //
    // this.map.on('click', this.giveCoordinate.bind(this));
    //
    // var map = L.map('map').setView([latitude, longitude], zoomLevel);

// Define the different tile layers (e.g., satellite and default map)
    var satelliteLayer = L.tileLayer('https://{s}.google.com/vt/lyrs=s&x={x}&y={y}&z={z}', {
      maxZoom: 20,
      subdomains: ['mt0', 'mt1', 'mt2', 'mt3'],
      attribution: 'Google Satellite'
    });

    var defaultLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: 'OpenStreetMap'
    });

// Add the default layer to the map
    defaultLayer.addTo(this.map);

// Create an object with layer names and corresponding layers
    var baseLayers = {
      "Satellite": satelliteLayer,
      "Default": defaultLayer
    };

// Add layer control to switch between different layers
    L.control.layers(baseLayers).addTo(this.map);



  }

  drwn(e: any) {
    this.editing = false;
    var type = e.layerType,
        layer = e.layer;
    this.drawnItems.addLayer(layer);
  }
  polyEditing(e:any) {
    if(!this.editing){ // do not start multiple "edit sessions"
      const editing = true;
      var polyEdit = new L.Draw.Polygon(this.map);
      polyEdit.enable();
      polyEdit.addVertex(e.latlng);
    }

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
    this.addPolygon()

  }
   tryArray:any[] =[
    [
      52.54266132492521,
      13.350560198412687
    ],
    [
      52.542662990579316,
      13.350566402976161
    ],
    [
      52.54266632589355,
      13.350578763057989
    ],
    [
      52.542669663679824,
      13.350591057120587
    ],
    [
      52.542673021447385,
      13.350603349647297
    ],
    [
      52.542676428072326,
      13.35061575079532
    ],
    [
      52.54267990284541,
      13.350628360545052
    ],
    [
      52.542683433634195,
      13.35064112266777
    ],
    [
      52.54268702608742,
      13.35065409068529
    ],
    [
      52.54269061085705,
      13.35066704040756
    ],
    [
      52.542692396377646,
      13.350673505555964
    ],
    [
      52.54267014767491,
      13.350689408569476
    ],
    [
      52.54266836215518,
      13.350682943423658
    ],
    [
      52.542664774198705,
      13.350669981696244
    ],
    [
      52.54266117885026,
      13.350657002768614
    ],
    [
      52.54265764587618,
      13.350644232400391
    ],
    [
      52.542654170471046,
      13.350631620267025
    ],
    [
      52.54265076536744,
      13.350619224858999
    ],
    [
      52.542647410585644,
      13.350606943582696
    ],
    [
      52.542644076318005,
      13.350594662772565
    ],
    [
      52.542640744473424,
      13.350582315748097
    ],
    [
      52.542639078820166,
      13.350576111187117
    ],
    [
      52.54266132492521,
      13.350560198412687
    ]
  ]
  addPolygon() {
    const originalCoords:any = this.tryArray

    const originalPolygon = L.polygon(originalCoords).addTo(this.map);

    // Simplify the polygon using turf.js
    const originalGeoJSON = turf.polygon([originalCoords]);
    const tolerance = 0.00000005; // Adjust the tolerance as needed
    const simplifiedGeoJSON = turf.simplify(originalGeoJSON, { tolerance });
    const simplifiedCoords: any = simplifiedGeoJSON.geometry.coordinates[0];
    simplifiedCoords.forEach((element:any) => {
      var marker = this.createMarkerPersistentStorage(element)


    });

    const simplifiedPolygon = L.polygon(simplifiedCoords, { color: 'blue' }).addTo(this.map);
    simplifiedPolygon.on('click', this.editing2.bind(this))
    console.log(originalCoords)
    console.log(simplifiedCoords)
    const newTol = 0
    const simplifiedCoordschanged =  turf.polygon([simplifiedCoords]);
    const revertedPolygon = turf.simplify(simplifiedCoordschanged, { tolerance:newTol });
    console.log("reverted ",revertedPolygon)
  }
  editing2(e: any) {
    console.log("polygonis", e)
    e.target.editing.enable();

   }
  createMarkerPersistentStorage(latlng: L.LatLng): L.Marker {
    var marker = L.marker(latlng, this.markerIcon).addTo(this.map);
    marker.on('drag', function (e: any) {
      console.log('dragging marker');
      console.log('marker position ', e.latlng);
    });
    return marker;
    // this.markerLayer.push(marker);
  }

}
