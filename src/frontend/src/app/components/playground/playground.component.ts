import { Component } from '@angular/core';
import * as L from 'leaflet';
import { RestAPIService } from 'src/app/services/restAPI/rest-api.service';
import 'leaflet-draw';
import * as turf from '@turf/turf';
import { PlayStorageService } from 'src/app/services/playStorage/play-storage.service';

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
  drawnItems: any


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

  timestamp_data=[
    {
      "id": 321,
      "parkingPointId": 313,
      "timestamp": "24.10.2023 07:23:43"
    },
    {
      "id": 4202,
      "parkingPointId": 313,
      "timestamp": "01.11.2023 08:04:50"
    },
    {
      "id": 4273,
      "parkingPointId": 313,
      "timestamp": "18.09.2023 06:50:57"
    },
    {
      "id": 27124,
      "parkingPointId": 313,
      "timestamp": "17.10.2023 06:59:30"
    },
    {
      "id": 36975,
      "parkingPointId": 313,
      "timestamp": "25.10.2023 07:14:57"
    }
  ]
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

    // this.checkOverlappingPolygon()


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

  constructor(private restApi: RestAPIService, private storage: PlayStorageService) {}

  ngAfterViewInit(): void {
    this.parkingSpaceData= this.storage.getData()
    this.initMap();
    this.addPolygon()
    // this.checkOverlappingPolygon()


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
      // var marker = this.createMarkerPersistentStorage(element)


    });

    const simplifiedPolygon = L.polygon(simplifiedCoords, { color: 'blue' }).addTo(this.map);
    // simplifiedPolygon.on('click', this.editing2.bind(this))
    console.log(originalCoords)
    console.log(simplifiedCoords)
    const newTol = 0
    const simplifiedCoordschanged =  turf.polygon([simplifiedCoords]);
    const revertedPolygon = turf.simplify(simplifiedCoordschanged, { tolerance:newTol });
    console.log("reverted ",revertedPolygon)

    console.log()
    console.log("------------------------------------------")

    const centerCoords = [52.54266132492521, 13.350560198412687];

// Define the vertices for the first polygon
    const polygon1Vertices :any = [
      [centerCoords[0] - 0.01, centerCoords[1] + 0.01],
      [centerCoords[0] - 0.01, centerCoords[1] - 0.01],
      // [centerCoords[0] + 0.01, centerCoords[1] - 0.01],
      [centerCoords[0] + 0.001, centerCoords[1] + 0.001]
    ];

// Define the vertices for the second polygon
    const polygon2Vertices:any = [
      [centerCoords[0] - 0.01, centerCoords[1] - 0.01],
      [centerCoords[0] - 0.01, centerCoords[1] + 0.01],
      [centerCoords[0] + 0.01, centerCoords[1] + 0.01],
    ];

// Create Leaflet polygons
    const polygon1 = L.polygon(polygon1Vertices, { color: 'blue' }).addTo(this.map);
    const polygon2 = L.polygon(polygon2Vertices, { color: 'green' }).addTo(this.map);

    const coords1:any = polygon1.getLatLngs()[0]; // Assuming a simple polygon, adjust accordingly
    const coords2 :any = polygon2.getLatLngs()[0];
    console.log("coorda",coords1)
    // Calculate the centroid of each polygon
    const centroid1 = turf.centroid(polygon1.toGeoJSON());
    const centroid2 = turf.centroid(polygon2.toGeoJSON());

// Calculate the translation vector to move the first polygon to the midpoint
    const translationVector = [
      centroid2.geometry.coordinates[0] - centroid1.geometry.coordinates[0],
      centroid2.geometry.coordinates[1] - centroid1.geometry.coordinates[1],
    ];

// Translate the vertices of the first polygon
    const shiftedPolygon1Vertices = polygon2Vertices.map((point:any) => [
      point[0] + translationVector[0]/2,
      point[1] + translationVector[1]/2,
    ]);

// Create a Leaflet polygon for the shifted first polygon
    const shiftedPolygon1 = L.polygon(shiftedPolygon1Vertices, { color: 'red' }).addTo(this.map);
    const meanCoords:any = [];
    const coordst1 = turf.explode(polygon1.toGeoJSON()).features.map(feature => feature.geometry.coordinates);
    const coordst2 = turf.explode(polygon2.toGeoJSON()).features.map(feature => feature.geometry.coordinates);
    console.log(coordst1)
    console.log(coordst2)
    for (let i = 0; i < Math.min(coordst1.length, coordst2.length); i++) {
      const lat = (coordst1[i][0] + coordst2[i][0]) / 2;
      const lng = (coordst1[i][1] + coordst2[i][1]) / 2;
      console.log(i, " lat :", lat, "lng: ", lng)
      meanCoords.push([lng, lat]);
    }

// Create a new Leaflet polygon using the mean coordinates
    const meanPolygon = L.polygon(meanCoords,{color:"black"}).addTo(this.map);

// Add a custom popup content with a table
    const tablePopupContent = `
  <div>
    <table>
      <thead>
        <tr>
          <th>Park Place Occupied</th>
        </tr>
      </thead>
      <tbody>
        ${this.generateTableRows(this.timestamp_data)}
      </tbody>
    </table>

  </div>
`;
    simplifiedPolygon.bindPopup(tablePopupContent, { maxWidth: 300 })



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

  generateTableRows(timestampData: any[]): string {
    return timestampData.map(item => {
      const isOccupied = this.checkOccupancy(item.timestamp); // Implement your logic
      return `<tr><td>${item.timestamp}</td></tr>`;
    }).join('');
  }
  checkOccupancy(timestamp: string): boolean {
    // Implement your logic to check if the parking place is occupied based on the timestamp
    // For example, you can compare the timestamp with the current time
    const currentTimestamp = new Date();
    const parkingTimestamp = new Date(timestamp);
    return parkingTimestamp <= currentTimestamp;
  }


 /* checkOverlappingPolygon() {
    this.parkingSpaceData = this.storage.getData()
    console.log("starting check")
    console.log(this.parkingSpaceData)


    this.parkingSpaceData.forEach((element: any) => {
      const element1 = element;
      const poly1 = turf.polygon([element1.simplified_initial_coordinates])
      console.log("poly1", poly1)
      // TODO get data as per the indices
      this.parkingSpaceData.forEach((element2: any,index:number) => {

        const poly2 = turf.polygon([element2.simplified_initial_coordinates])
        // console.log("poly2", poly2)
        const flag_intersection = turf.booleanOverlap(poly1, poly2)
        console.log("Intersecting or not ",flag_intersection, poly1, poly2)
        if (flag_intersection) {
          console.error("Intersecting or not ",flag_intersection, poly1, poly2)
        }


      })

    });

  }
*/
}
