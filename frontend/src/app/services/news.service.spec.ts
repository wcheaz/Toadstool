import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NewsService } from './news.service';

describe('NewsService routing', () => {
  let service: NewsService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(NewsService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends news requests to the same-origin API', () => {
    service.getMarketNews().subscribe();

    const request = http.expectOne('/api/news/market');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('preserves query parameters on same-origin requests', () => {
    service.getLatestNews(['AAPL', 'MSFT']).subscribe();

    const request = http.expectOne('/api/news/latest?symbols=AAPL,MSFT');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });
});
