import {LineChart} from "../../layout/property/property.component";

export class Property {
    id:         string;
    city:       string;
    address:    string;
    postcode:   string;
    coordinate: Coordinates;
    extra:      Extra[];
    strExtra:   string;
    prices:     Price[];
    chart:      LineChart;

    constructor() {
        this.extra = [];
        this.prices = [];
        this.coordinate = new Coordinates();
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
    date:   string;
    price:  number;

    constructor() { }
}

