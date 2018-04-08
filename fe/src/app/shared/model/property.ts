import {LineChart} from "../../layout/property/property.component";

export class Property {
    _id:         string;
    city:       string;
    address:    string;
    postcode:   string;
    coordinates:Coordinates;
    extra:      Extra[];
    strExtra:   string;
    prices:     Price[];
    chart:      LineChart;

    constructor() {
        this.extra = [];
        this.prices = [];
        this.coordinates = new Coordinates();
    }
}

export class Coordinates {
    lat:    number;
    long:   number;

    constructor() { }
}

export class Extra {
    name:  string;
    value: number;

    constructor( ) { }
}

export class Price {
    data:   Date;
    value:  number;

    constructor() { }
}

