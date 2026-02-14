export interface LocationReport {
  id: number;
  reportDate: string;
  country: string;
  port: string;
}

export interface LocationReportRequest {
  reportDate: string;
  country: string;
  port: string;
}
