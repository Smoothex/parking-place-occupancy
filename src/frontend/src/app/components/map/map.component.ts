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
  private markerLayer: any = [];
  private parkingSpaceData: any=[];



  markerIcon = {
    draggable:true,
    icon: L.icon({
      iconSize: [25, 41],
      iconAnchor: [12.5, 41],
      // specify the path here
      iconUrl: '../../assets/marker-icon.png',
      shadowUrl: '../../assets/marker-shadow.png',
    }),
  };
  polygonColor: string ='green'

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
            parseElement.polygon.coordinates = arrayOfArrays
            let obj = {...
              parseElement,
              // "marker":arrayOfArrays
            }
            this.parkingSpaceData.push(obj)

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
              this.polygonColor = 'red' 
            }
            polygon.setStyle({fillColor: this.polygonColor,color: this.polygonColor});
            polygon.on('click', (event) => {
              console.log(event + 'parse' + parseElement.id);
              polygon.bringToFront()
              arrayOfArrays.forEach((elem: any) => {
                // const marker = this.createMarkerPersistentStorage(elem)
                // this.parkingSpaceData[0]
                console.log(polygon)
                
              });
            });
          });
      })
      console.log(this.parkingSpaceData)

    })
      
  }
  /**
   * Event handler activated while changing the polygon shape
   * @param latlng geolocation of marker on the map
   */
  createMarkerPersistentStorage(latlng: L.LatLng): L.Marker{
    var marker = L.marker(latlng, this.markerIcon).addTo(this.map);
    marker.on('drag', function (e: any) {
      console.log("dragging marker")
      console.log("marker position ",e.latlng)
    })
    return marker
    // this.markerLayer.push(marker);
    
  }





}
