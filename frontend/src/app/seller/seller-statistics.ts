import { Component, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RequestService, SellerDashboardStats } from '../core/services/request.service';
// @ts-ignore
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-seller-statistics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './seller-statistics.html'
})
export class SellerStatistics implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('offersChart') offersChartRef!: ElementRef<HTMLCanvasElement>;

  private requestService = inject(RequestService);

  stats: SellerDashboardStats | null = null;
  isLoading = true;

  private revenueChartInstance: Chart | null = null;
  private offersChartInstance: Chart | null = null;

  readonly monthLabels = ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Jun', 'Jul', 'Aou', 'Sep', 'Oct', 'Nov', 'Dec'];
  readonly currentYear = new Date().getFullYear();

  ngOnInit(): void {
    this.fetchStats();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.revenueChartInstance?.destroy();
    this.offersChartInstance?.destroy();
  }

  fetchStats(): void {
    this.isLoading = true;
    this.requestService.getSellerStats().subscribe({
      next: (s) => {
        this.stats = s;
        this.isLoading = false;
        setTimeout(() => this.renderCharts(), 50);
      },
      error: () => { this.isLoading = false; }
    });
  }

  get conversionRate(): number {
    if (!this.stats || this.stats.totalOffers === 0) return 0;
    return Math.round((this.stats.acceptedOffers / this.stats.totalOffers) * 100);
  }

  get avgOrderValue(): number {
    if (!this.stats || this.stats.acceptedOffers === 0) return 0;
    return Math.round(this.stats.totalRevenue / this.stats.acceptedOffers);
  }

  private renderCharts(): void {
    if (!this.stats) return;
    this.renderRevenueChart();
    this.renderOffersChart();
  }

  private renderRevenueChart(): void {
    if (!this.revenueChartRef?.nativeElement || !this.stats) return;
    this.revenueChartInstance?.destroy();

    const ctx = this.revenueChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    const gradient = ctx.createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(37, 83, 98, 0.25)');
    gradient.addColorStop(1, 'rgba(37, 83, 98, 0.02)');

    this.revenueChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.monthLabels,
        datasets: [{
          label: 'Revenu (MAD)',
          data: this.stats.monthlyRevenue,
          borderColor: '#255362',
          backgroundColor: gradient,
          borderWidth: 2.5,
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointBackgroundColor: '#255362',
          pointBorderColor: '#fff',
          pointBorderWidth: 2,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1a1a2e',
            titleFont: { weight: 'bold', size: 12 },
            bodyFont: { size: 13 },
            padding: 12,
            cornerRadius: 8,
            callbacks: {
              label: (ctx: any) => `${ctx.parsed.y.toLocaleString()} MAD`
            }
          }
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { font: { size: 11, weight: 'bold' }, color: '#9ca3af' }
          },
          y: {
            beginAtZero: true,
            grid: { color: '#f3f4f6' },
            ticks: {
              font: { size: 11, weight: 'bold' },
              color: '#9ca3af',
              callback: (value: any) => value.toLocaleString() + ' MAD'
            }
          }
        }
      }
    });
  }

  private renderOffersChart(): void {
    if (!this.offersChartRef?.nativeElement || !this.stats) return;
    this.offersChartInstance?.destroy();

    const ctx = this.offersChartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    this.offersChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Acceptees', 'En attente', 'Rejetees', 'Annulees'],
        datasets: [{
          data: [
            this.stats.acceptedOffers,
            this.stats.pendingOffers,
            this.stats.rejectedOffers,
            this.stats.cancelledOffers
          ],
          backgroundColor: ['#059669', '#d97706', '#dc2626', '#9ca3af'],
          borderWidth: 0,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 16,
              usePointStyle: true,
              pointStyle: 'circle',
              font: { size: 11, weight: 'bold' }
            }
          },
          tooltip: {
            backgroundColor: '#1a1a2e',
            padding: 12,
            cornerRadius: 8,
            bodyFont: { size: 13 }
          }
        }
      }
    });
  }
}
