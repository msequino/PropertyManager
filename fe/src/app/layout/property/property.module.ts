import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';

import {PropertyComponent} from './property.component';
import {StatModule} from "../../shared";
import {NgbModule} from "@ng-bootstrap/ng-bootstrap";
import {PropertyRoutingModule} from "./property-routing.module";
import {PropertyService} from "../../shared/services";

import {ChartsModule as Ng2Charts} from "ng2-charts";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";

@NgModule({
  imports: [
      CommonModule,
      NgbModule.forRoot(),
      PropertyRoutingModule,
      StatModule,
      Ng2Charts,
      ReactiveFormsModule,
      FormsModule
  ],
  declarations: [PropertyComponent],
  providers : [ PropertyService ]
})
export class PropertyModule { }
