

export interface ParkingSpace {
    id:       number;
    polygon:  Polygon;
    occupied: boolean;
}

export interface Polygon {
    coordinates: CoordinateBackend[];
}

export interface CoordinateBackend {
    x: number;
    y: number;
}
