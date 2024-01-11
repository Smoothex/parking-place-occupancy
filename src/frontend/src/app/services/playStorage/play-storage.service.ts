import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class PlayStorageService {

  constructor() { }

  parkingSpaceData: any
  storeData(data:any) {
    this.parkingSpaceData = data
    console.log("service", this.parkingSpaceData)

  }
  
  getData() {
    return this.parkingSpaceData
  }
}
                       