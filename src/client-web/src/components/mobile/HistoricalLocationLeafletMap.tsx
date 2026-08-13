import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { MapContainer, Marker, Polyline, Popup, TileLayer } from 'react-leaflet';
import type { MobileLocationPathPoint, MobileLocationTrack } from '../../api/mobile';
import { formatDateTime } from './mobileFormatting';
import {
  formatAccuracyLabel,
  formatCoordinate,
  formatDistanceMeters,
  segmentKindLabel,
} from './locationFormatting';

export interface HistoricalLocationLeafletMapProps {
  tracks: MobileLocationTrack[];
  selectedSegmentId?: string | null;
  selectedPointId?: string | null;
  onSelectSegment?: (segmentId: string) => void;
  onSelectPoint?: (pointId: string) => void;
}

const markerIcon = L.divIcon({
  className: 'pim-location-marker',
  html: '<span></span>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

const selectedMarkerIcon = L.divIcon({
  className: 'pim-location-marker pim-location-marker-selected',
  html: '<span></span>',
  iconSize: [24, 24],
  iconAnchor: [12, 12],
});

function allSegments(tracks: MobileLocationTrack[]) {
  return tracks.flatMap(track => track.segments);
}

function pathPosition(point: MobileLocationPathPoint): [number, number] {
  return [point.latitude, point.longitude];
}

function firstPosition(tracks: MobileLocationTrack[]): [number, number] {
  const point = allSegments(tracks).flatMap(segment => segment.path)[0];
  return point ? pathPosition(point) : [31.2304, 121.4737];
}

function segmentColor(kind: string, selected: boolean) {
  if (selected) return '#e11d48';
  return kind === 'move' ? '#2563eb' : '#14b8a6';
}

export default function HistoricalLocationLeafletMap({
  tracks,
  selectedSegmentId,
  selectedPointId,
  onSelectSegment,
  onSelectPoint,
}: HistoricalLocationLeafletMapProps) {
  const segments = allSegments(tracks);

  return (
    <MapContainer center={firstPosition(tracks)} zoom={segments.length > 0 ? 13 : 5} className="h-full min-h-[420px] w-full">
      {/* 瓦片走同域 /tiles 中转（生产由服务器 nginx 反代 tile.openstreetmap.org），
          避免直连 OSM 官方瓦片在国内不稳定；根相对路径不依赖部署域名。 */}
      <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="/tiles/{z}/{x}/{y}.png"
      />
      {segments.map(segment => {
        const selected = segment.id === selectedSegmentId;
        const positions = segment.path.map(pathPosition);
        return (
          <Polyline
            key={segment.id}
            positions={positions}
            pathOptions={{
              color: segmentColor(segment.kind, selected),
              weight: selected ? 5 : 3,
              dashArray: segment.kind === 'move' ? undefined : '8 8',
            }}
            eventHandlers={{ click: () => onSelectSegment?.(segment.id) }}
          />
        );
      })}
      {segments.flatMap(segment => segment.path.map((point, index) => ({ segment, point, index }))).map(({ segment, point, index }) => {
        const pointId = point.id ?? `${segment.id}-point-${index}`;
        const selected = pointId === selectedPointId;
        return (
          <Marker
            key={pointId}
            position={pathPosition(point)}
            icon={selected ? selectedMarkerIcon : markerIcon}
            opacity={selected ? 1 : 0.76}
            eventHandlers={{ click: () => {
              onSelectSegment?.(segment.id);
              onSelectPoint?.(pointId);
            } }}
          >
            <Popup>
              <div>
                <strong>{segmentKindLabel(segment.kind)}片段</strong>
                <br />
                {point.recordedAtUtc ? formatDateTime(point.recordedAtUtc) : `${segment.localStart} 至 ${segment.localEnd}`}
                <br />
                {formatCoordinate(point.latitude, point.longitude)}
                <br />
                误差 {formatAccuracyLabel(point.horizontalAccuracyMeters)}
                <br />
                里程 {formatDistanceMeters(segment.distanceMeters)}
              </div>
            </Popup>
          </Marker>
        );
      })}
    </MapContainer>
  );
}
