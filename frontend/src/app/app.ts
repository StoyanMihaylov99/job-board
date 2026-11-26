import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './auth/auth.service';
import { Announcer } from './shared/announcer/announcer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  protected readonly announcer = inject(Announcer);
  protected readonly auth = inject(AuthService);

  protected signOut(): void {
    void this.auth.logout();
  }
}
