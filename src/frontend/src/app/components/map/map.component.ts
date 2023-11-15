import { Component } from '@angular/core';
import * as L from 'leaflet';
import { RestAPIService } from 'src/app/services/restAPI/rest-api.service';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent {

  private map:any;

  private initMap(): void {
    this.map = L.map('map').setView([52.54412, 13.352], 15);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
    maxZoom: 24,
    attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(this.map);
    
    this.map.on('click',this.giveCoordinate)

  }

  giveCoordinate(e: any) {
    console.log("clicked on map", e.latlng)
    var marker = L.marker(e.latlng)
      marker.addTo(this.map)
    
  }

  constructor(private restApi: RestAPIService) { }

  ngAfterViewInit(): void {
    this.initMap();
    setTimeout(() => {
      
      this.markParkingPlaces()
    }, 500);
  }
  

  markParkingPlaces(): void{
    this.restApi.getAllParkingSpaces().then(data => {
      data.forEach((element: any) => {

        const parseElement = JSON.parse(element)
        console.log(parseElement)
        var coordinatesArray = parseElement.polygon.coordinates
        const arrayOfArrays = coordinatesArray.map((obj:any) => [obj.y, obj.x]);
        console.log(arrayOfArrays)
        var polygon = L.polygon(arrayOfArrays).addTo(this.map)
          .bindPopup("Id " + parseElement.id + "<br>" + "occupied : " + JSON.stringify(parseElement.occupied));
       /*  polygon.on('click',  (event) =>{
          console.log(event + "parse" + parseElement.id)
          arrayOfArrays.forEach((elem:any) => {
            L.marker(elem).addTo(this.map)
          })
        }) */
      });

      

    })
    
  }


}
