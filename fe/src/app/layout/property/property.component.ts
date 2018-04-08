import {Component, OnInit} from '@angular/core';
import {PropertyService} from "../../shared/services/index";
import {Extra, Price, Property} from "../../shared/model/property";

import {NgbModal} from '@ng-bootstrap/ng-bootstrap';
import {routerTransition} from "../../router.animations";
import {FormArray, FormBuilder, FormGroup, Validators} from "@angular/forms";


@Component({
  selector: 'app-property',
  templateUrl: './property.component.html',
  styleUrls: ['./property.component.css'],
  animations: [routerTransition()]
})
export class PropertyComponent implements OnInit {

  public properties: Property[];
  public closeResult: string;

  public propertyForm: FormGroup;
  public priceForm: FormGroup;

  public maxdate : Date;


  constructor(private propertyService: PropertyService, private modalService: NgbModal, private fb: FormBuilder) {
      this.properties = [];
      this.maxdate = new Date();

      this.propertyForm = fb.group({
          id: [''],
          city: ['', Validators.required],
          address: ['', Validators.required],
          postcode: ['', Validators.required],
          lat: ['', Validators.required],
          long: ['', Validators.required],
          extra: fb.array([])
      });

      this.priceForm = fb.group({
          id: [''],
          date: ['', Validators.required],
          price: ['', Validators.required]
      });

  }

  createItem(name, value): FormGroup {
    return this.fb.group({
        name: [name, Validators.required],
        value: [value, Validators.required]
    });
  }

  addItem(name, value): void {
      (this.propertyForm.get("extra") as FormArray).push(this.createItem(name, value));
  }

  deleteItem(id: number): void {
      (this.propertyForm.get("extra") as FormArray).removeAt(id);
  }

  ngOnInit() {
      this.propertyService.getProperties().subscribe( data => {
          this.properties = data.map(d => {
              let prop = d as Property;
              prop.strExtra = prop.extra.map(d => (d.name + ": " + d.value)).join(",\n");
              prop.chart = new LineChart(prop.prices.map(d => d.value), prop.prices.map(d => d.data))
              return prop;
          });
      });

  }

  open(content, row, event) {

      event.stopPropagation()

      this.propertyForm.reset();

      // TODO manse does not exist a better way ?? omg
      for (let i = (this.propertyForm.get("extra") as FormArray).length; i >= 0 ; i--)
          (this.propertyForm.get("extra") as FormArray).removeAt(i);

      if(row != null) {
          this.priceForm.get("id").setValue(row._id);
          this.propertyForm.get("id").setValue(row._id);
          this.propertyForm.get("city").setValue(row.city);
          this.propertyForm.get("address").setValue(row.address);
          this.propertyForm.get("postcode").setValue(row.postcode);
          this.propertyForm.get("lat").setValue(row.coordinates.lat);
          this.propertyForm.get("long").setValue(row.coordinates.long);

          row.extra.forEach(d => {
            this.addItem(d.name, d.value);
          });
      }

      this.modalService.open(content, {"size": "lg"}).result.then((result) => {
          this.closeResult = `Closed with: ${result}`;
      }, (reason) => {
          this.closeResult = `Dismissed`;
      });
  }

  saveProperty() {
      // TODO manse save property
      let p : Property = new Property();
      p._id = this.propertyForm.get("id").value;
      p.city = this.propertyForm.get("city").value;
      p.address = this.propertyForm.get("address").value;
      p.postcode = this.propertyForm.get("postcode").value;
      p.coordinates.lat = this.propertyForm.get("lat").value;
      p.coordinates.long = this.propertyForm.get("long").value;

      (this.propertyForm.get("extra") as FormArray).value.forEach( d => {
          let e = new Extra();
          e.name = d.name;
          e.value = d.value;
          p.extra.push(e)
      })
      console.log(p);
      if(p._id == '' || typeof p._id == undefined)
          this.propertyService.addProperty(p);
      else
          this.propertyService.editProperty(p);
  }

  deleteProperty() {
      // TODO manse delete property
      this.propertyService.removeProperty(this.propertyForm.get("id").value);
  }

  submitPrice() {
      console.log(this.priceForm)
      // TODO manse delete property
      this.propertyService.addPrice(this.propertyForm.get("id").value, new Price());
  }

}

export class LineChart {
    constructor(data: number[], labels: Date[]) {
        this.lineChartData[0].data = data;
        this.lineChartLabels = labels;
    }
    public showable : boolean = false;

    public lineChartData: Array<any> = [
        { data: [65, 59, 80, 81, 56, 55, 40], label: 'Price over time' },
    ];
    public lineChartLabels: Array<any> = [
        'January',
        'February',
        'March',
        'April',
        'May',
        'June',
        'July'
    ];
    public lineChartOptions: any = {
        responsive: true
    };
    public lineChartColors: Array<any> = [
        {
            // grey
            backgroundColor: 'rgba(148,159,177,0.2)',
            borderColor: 'rgba(148,159,177,1)',
            pointBackgroundColor: 'rgba(148,159,177,1)',
            pointBorderColor: '#fff',
            pointHoverBackgroundColor: '#fff',
            pointHoverBorderColor: 'rgba(148,159,177,0.8)'
        }
    ];
    public lineChartLegend: boolean = true;
    public lineChartType: string = 'line';

}
