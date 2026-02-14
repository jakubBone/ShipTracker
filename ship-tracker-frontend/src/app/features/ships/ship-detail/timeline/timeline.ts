import { Component, Input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { LocationReport } from '../../../../core/models/location-report.model';

@Component({
  selector: 'app-timeline',
  imports: [DatePipe],
  templateUrl: './timeline.html',
  styleUrl: './timeline.scss'
})
export class Timeline {
  @Input() reports: LocationReport[] = [];
}
