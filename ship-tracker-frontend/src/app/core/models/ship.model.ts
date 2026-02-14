export interface Ship {
  id: number;
  name: string;
  launchDate: string;
  shipType: string;
  tonnage: number;
  reportCount: number;
}

export interface ShipRequest {
  name: string;
  launchDate: string;
  shipType: string;
  tonnage: number;
}
