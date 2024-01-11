import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from "@angular/common/http";
import { ParkingSpace } from 'src/app/interfaces/parking-space';
@Injectable({
  providedIn: 'root'
})
export class RestAPIService {

  apiURL:string="http://localhost:8080/api/"

  constructor(private http: HttpClient) { }

    // GET methods
    /**
     * @returns
     */
    getAllParkingSpaces(): Promise<any>{

      return new Promise((resolve) => {
        const options = {
          // headers: new HttpHeaders().set("APP-USER-ID", this.userId),
          // params: new HttpParams().set("param", tripid),
        };
        this.http.get(this.apiURL+"parking-spaces", options).pipe().subscribe({
          next: (data) => {resolve(data)},
          error: err => { console.error("error generated", err)}
        })
      })
    }

    /**
     *
     * @param id {id} of parking spaces to
     * @returns information about the parking space matched to id
     */
    getParkingSpaceWithId(id: string) {

      return new Promise((resolve) => {
        const options = {
          // headers: new HttpHeaders().set("APP-USER-ID", this.userId),
          // params: new HttpParams().set("param", tripid),
        };
        this.http.get(this.apiURL+"parking-spaces/"+id, options).pipe().subscribe({
          next: (data) => {resolve(data)},
          error: err => { console.error("error generated", err)}
        })
      })
    }

    /**
     *
     * @param id {id} of parking spaces to
     * @returns information about the parking space matched to id
     */
    getParkingSpaceAreaWithId(id: string) {

      return new Promise((resolve) => {
        const options = {
          // headers: new HttpHeaders().set("APP-USER-ID", this.userId),
          // params: new HttpParams().set("param", tripid),
        };
        this.http.get(this.apiURL+"parking-spaces/"+id+"/area", options).pipe().subscribe({
          next: (data) => {resolve(data)},
          error: err => { console.error("error generated", err)}
        })
      })
    }

    /**
     *
     * @param occupied Occupancy state of space
     * @returns the parking spaces which matched occupancy state
     */
    getAllParkingSpaceOccupied(occupied:boolean) {

      return new Promise((resolve) => {
        const options = {
          // headers: new HttpHeaders().set("APP-USER-ID", this.userId),
          params: new HttpParams().set("occupied", occupied),
        };
        this.http.get(this.apiURL+"parking-spaces/search", options).pipe().subscribe({
          next: (data) => {resolve(data)},
          error: err => { console.error("error generated", err)}
        })
      })
    }

    /**
     *
     * @param id {id} of parking spaces to
     * @param occupied Occupancy state of space
     * @returns the parking spaces which matched occupancy state
     */
    getParkingSpaceOccupied(id:string,occupied:boolean) {

      return new Promise((resolve) => {
        const options = {
          // headers: new HttpHeaders().set("APP-USER-ID", this.userId),
          params: new HttpParams().set("occupied", occupied),
        };
        this.http.get(this.apiURL+"parking-spaces/"+id+"/occupancy", options).pipe().subscribe({
          next: (data) => {resolve(data)},
          error: err => { console.error("error generated", err)}
        })
      })
    }

  updateParkingSpaceWithId(parkingSpaceId: string, parkingGeometry: any[]) {
    return new Promise((resolve) => {
      const options = {
        params: new HttpParams().set("occupied", parkingSpaceId),
      };
      const body = {
        geometry: parkingGeometry
      }
      this.http.post(this.apiURL+"parking-spaces/"+parkingSpaceId,body, options).pipe().subscribe({
        next: (data) => {resolve(data)},
        error: err => { console.error("error generated", err)}
      })
    })
    
  }


}
