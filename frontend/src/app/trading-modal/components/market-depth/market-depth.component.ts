import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DepthLevel } from '../../../core/api/api.models';

@Component({
  selector: 'app-market-depth',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './market-depth.component.html',
  styleUrl: './market-depth.component.css'
})
export class MarketDepthComponent {
  @Input() depthLevels: DepthLevel[] = [];
}
