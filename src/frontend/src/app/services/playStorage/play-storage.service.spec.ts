import { TestBed } from '@angular/core/testing';

import { PlayStorageService } from './play-storage.service';

describe('PlayStorageService', () => {
  let service: PlayStorageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PlayStorageService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
