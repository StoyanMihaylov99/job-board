import { ChangeDetectionStrategy, Component } from '@angular/core';

/** What Job Board is for, shown below the sign-in and sign-up forms. */
@Component({
  selector: 'app-about-section',
  templateUrl: './about-section.html',
  styleUrl: './about-section.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AboutSection {}
