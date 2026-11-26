import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthResponse, User } from '../auth/auth.model';

const ME_URL = '/api/v1/me';

@Injectable({ providedIn: 'root' })
export class ProfileApi {
  private readonly http = inject(HttpClient);

  updateDisplayName(displayName: string): Promise<User> {
    return firstValueFrom(this.http.patch<User>(ME_URL, { displayName }));
  }

  /** Ends every other session; the response carries a fresh one for this browser. */
  changePassword(currentPassword: string, newPassword: string): Promise<AuthResponse> {
    return firstValueFrom(
      this.http.put<AuthResponse>(`${ME_URL}/password`, { currentPassword, newPassword }),
    );
  }
}
