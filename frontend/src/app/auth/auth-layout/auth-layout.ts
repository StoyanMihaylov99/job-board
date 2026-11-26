import { ChangeDetectionStrategy, Component } from '@angular/core';

import { AboutSection } from '../about-section/about-section';

/**
 * Two-part layout for sign-in and sign-up. The form comes first in the DOM so it sits at the top on
 * phones (above the keyboard); on wide screens the introduction moves to the left.
 */
@Component({
  selector: 'app-auth-layout',
  imports: [AboutSection],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLayout {}
