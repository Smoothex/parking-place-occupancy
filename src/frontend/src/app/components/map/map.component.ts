import { Component } from '@angular/core';
import * as L from 'leaflet';
import { RestAPIService } from 'src/app/services/restAPI/rest-api.service';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css'],
})
export class MapComponent {
  private map: any;
  private markerLayer: any=[]

  icon = {
    icon: L.icon({
      iconSize: [25, 41],
      iconAnchor: [12.5, 41],
      // specify the path here
      iconUrl: '../../assets/marker-icon.png',
      shadowUrl: '../../assets/marker-shadow.png',
    }),
  };

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
    this.map.removeLayer(this.markerLayer)
  }

  constructor(private restApi: RestAPIService) {}

  ngAfterViewInit(): void {
    this.initMap();
    setTimeout(() => {
      this.markParkingPlaces();
    }, 500);
  }

  markParkingPlaces(): void {
    this.restApi.getAllParkingSpaces().then((data) => {
      data.forEach((element: any) => {
        const parseElement = JSON.parse(element);
        console.log(parseElement);
        //calculate area 
        // todo: add models to the remaining structure
        this.restApi.getParkingSpaceAreaWithId(parseElement.id).then((areaData)=>{
          // console.log(areaData)
          return areaData;
        }, (error) => {
          
        }).then((area) => {

          var coordinatesArray = parseElement.polygon.coordinates;
          const arrayOfArrays = coordinatesArray.map((obj: any) => [
            obj.y,
            obj.x,
          ]);
          console.log(arrayOfArrays);
          var polygon = L.polygon(arrayOfArrays)
            .addTo(this.map)
            .bindPopup(
              'Id ' +
                parseElement.id +
                '<br>' +
                'occupied : ' +
              JSON.stringify(parseElement.occupied) +
              " <br>Area : "+area+
                '<br> <button>Edit</button></button>'
            );
          polygon.on('click', (event) => {
            console.log(event + 'parse' + parseElement.id);
            arrayOfArrays.forEach((elem: any) => {
              var marker = L.marker(elem, this.icon).addTo(this.map);
              this.markerLayer.push(marker)
            });
          });
          



        })

      
      });
    });
  }
}
