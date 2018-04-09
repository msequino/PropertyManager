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

  public modalRef ;

  constructor(private propertyService: PropertyService, private modalService: NgbModal, private fb: FormBuilder) {
      this.properties = [];

      this.propertyForm = fb.group({
          id: [''],
          city: ['', Validators.required],
          address: ['', Validators.required],
          postcode: ['', Validators.required],
          lat: ['', Validators.required],
          long: ['', Validators.required],
          extra: fb.array([]),
          prices: fb.array([])
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

  createPrice(name, value): FormGroup {
      return this.fb.group({
        date: [name, Validators.required],
        price: [value, Validators.required]
    });
  }

  addPrice(name, value): void {
    (this.propertyForm.get("prices") as FormArray).push(this.createPrice(name, value));
  }

  addItem(name, value): void {
    (this.propertyForm.get("extra") as FormArray).push(this.createItem(name, value));
  }

  deleteItem(id: number): void {
    (this.propertyForm.get("extra") as FormArray).removeAt(id);
  }

  loadPage() {
      this.propertyService.getProperties().subscribe( data => {
          this.properties = data.map(d => {
              let prop = d as Property;
              prop.strExtra = (prop.extra ? prop.extra.map(d => (d.name + ": " + d.value)).join(",\n") : "");
              prop.prices.sort((a,b) => new Date(a.date).getTime() - new Date(b.date).getTime())
              prop.chart = (prop.prices ? new LineChart(prop.prices.map(d => d.price), prop.prices.map(d => new Date(d.date))) : null);
              return prop;
          });
      });
  }

  ngOnInit() {

      this.loadPage();
  }

  open(content, row, event) {

      event.stopPropagation();

      this.propertyForm.reset();

      // TODO manse does not exist a better way ?? omg
      for (let i = (this.propertyForm.get("extra") as FormArray).length; i >= 0 ; i--)
          (this.propertyForm.get("extra") as FormArray).removeAt(i);

      if(row != null) {
          this.priceForm.get("id").setValue(row.id);
          this.propertyForm.get("id").setValue(row.id);
          this.propertyForm.get("city").setValue(row.city);
          this.propertyForm.get("address").setValue(row.address);
          this.propertyForm.get("postcode").setValue(row.postcode);
          this.propertyForm.get("lat").setValue(row.coordinate.lat);
          this.propertyForm.get("long").setValue(row.coordinate.long);

          if(row.extra)
              row.extra.forEach(d => {
                this.addItem(d.name, d.value);
              });
          if(row.prices)
              row.prices.forEach(d => {
                  this.addPrice(d.date, d.price);
              });

      }

      this.modalRef = this.modalService.open(content, {"size": "lg"})
      this.modalRef.result.then((result) => {
          this.closeResult = `Closed with: ${result}`;
      }, (reason) => {
          this.closeResult = `Dismissed`;
      });
  }

  saveProperty() {

      let p : Property = new Property();
      p.id = this.propertyForm.get("id").value;
      p.city = this.propertyForm.get("city").value;
      p.address = this.propertyForm.get("address").value;
      p.postcode = this.propertyForm.get("postcode").value;
      p.coordinate.lat = this.propertyForm.get("lat").value;
      p.coordinate.long = this.propertyForm.get("long").value;

      (this.propertyForm.get("prices") as FormArray).value.forEach( d => {
          let e = new Price();
          e.date = d.date;
          e.price = d.price;
          p.prices.push(e)
      });

      (this.propertyForm.get("extra") as FormArray).value.forEach( d => {
          let e = new Extra();
          e.name = d.name;
          e.value = d.value;
          p.extra.push(e)
      });

      if(p.id == '' || p.id == null) {
          this.propertyService.addProperty(p).subscribe(res => {
              this.properties = [];
              this.loadPage();
              this.modalRef.close();
          });
      } else
          this.propertyService.editProperty(p).subscribe(res => {
              this.properties = [];
              this.loadPage();
              this.modalRef.close();
          });
  }

  deleteProperty() {
      let p : Property = new Property();
      p.id = this.propertyForm.get("id").value;
      p.city = this.propertyForm.get("city").value;
      p.address = this.propertyForm.get("address").value;
      p.postcode = this.propertyForm.get("postcode").value;
      p.coordinate.lat = this.propertyForm.get("lat").value;
      p.coordinate.long = this.propertyForm.get("long").value;

      (this.propertyForm.get("extra") as FormArray).value.forEach( d => {
          let e = new Extra();
          e.name = d.name;
          e.value = d.value;
          p.extra.push(e)
      })

      this.propertyService.removeProperty(p).subscribe(d => {

          this.properties = [];
          this.loadPage();
          this.modalRef.close();

      });
  }

  submitPrice() {
      console.log(this.priceForm)
      let price = new Price();
      let tmp = this.priceForm.get("date").value;
      price.date = tmp.year + "-" + tmp.month + "-" + tmp.day;
      price.price = this.priceForm.get("price").value;

      this.propertyService.addPrice(this.propertyForm.get("id").value, price).subscribe( res => {

          this.priceForm.reset();
          this.properties = [];
          this.loadPage();
          this.modalRef.close();
      });
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
