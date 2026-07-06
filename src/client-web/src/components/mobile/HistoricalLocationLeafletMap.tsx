import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { MapContainer, Marker, Polyline, Popup, TileLayer } from 'react-leaflet';
import type { MobileLocationPoint } from '../../api/mobile';
import { formatDateTime } from './mobileFormatting';
import { formatAccuracyLabel, formatCoordinate, locationQualityLabel } from './locationFormatting';

export interface HistoricalLocationLeafletMapProps {
  points: MobileLocationPoint[];
  selectedPointId?: string | null;
  onSelectPoint?: (pointId: string) => void;
}

const markerIcon = L.divIcon({
  className: 'pim-location-marker',
  html: '<span></span>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

export default function HistoricalLocationLeafletMap({
  points,
  selectedPointId,
  onSelectPoint,
}: HistoricalLocationLeafletMapProps) {
  const positions = points.map(point => [point.latitude, point.longitude] as [number, number]);
  const center = positions[0] ?? [31.2304, 121.4737];

  return (
    <MapContainer center={center} zoom={positions.length > 0 ? 13 : 5} className="h-full min-h-[360px] w-full">
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />
      {positions.length > 1 && <Polyline positions={positions} pathOptions={{ color: '#2563eb', weight: 3 }} />}
      {points.map(point => (
        <Marker
          key={point.id}
          position={[point.latitude, point.longitude]}
          icon={markerIcon}
          opacity={point.id === selectedPointId ? 1 : 0.75}
          eventHandlers={{ click: () => onSelectPoint?.(point.id) }}
        >
          <Popup>
            <div>
              <strong>{formatDateTime(point.recordedAtUtc)}</strong>
              <br />
              {formatCoordinate(point.latitude, point.longitude)}
              <br />
              误差 {formatAccuracyLabel(point.horizontalAccuracyMeters)}
              <br />
              质量 {locationQualityLabel(point.quality)}
            </div>
          </Popup>
        </Marker>
      ))}
    </MapContainer>
  );
}
