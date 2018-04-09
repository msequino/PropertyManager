import { Injectable } from '@angular/core';
import {Price, Property} from "../model/property";

import { Observable } from "rxjs/Observable";
import {HttpClient} from "@angular/common/http";
import 'rxjs/Rx';
import { environment} from "../../../environments/environment.prod";

@Injectable()
export class PropertyService {

    private host : string = environment.proto + "://" + environment.host + ":" + environment.port;
    constructor(private http: HttpClient) { }

    getProperties() : Observable<Property[]> {
        return this.http
            .get(this.host + "/api/v1/property").map(data => data as Property[])
    }

    addProperty(data: Property) : Observable<Property> {
        return this.http
            .post(this.host + "/api/v1/property", data).map(res => res as Property)
    }

    editProperty(data: Property) : Observable<Property> {
        return this.http
            .put(this.host + "/api/v1/property?id=" + data.id, data).map(res => res as Property)
    }

    removeProperty(data: Property) : Observable<Boolean> {
        return this.http
            .post(this.host + "/api/v1/property/delete", {id: data.id}).map(res => res as Boolean)
    }

    addPrice(id: string, data: Price) : Observable<Property> {
        return this.http
            .post(this.host + "/api/v1/price/property?id=" + id, data).map(res => res as Property)
    }


}
