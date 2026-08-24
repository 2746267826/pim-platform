-- Repro: overlap double count in pim_test
INSERT INTO pc_aw_events (id, bucket_id, event_type, app_name, timestamp, duration, data_json, source_event_id)
VALUES (gen_random_uuid(), (SELECT id FROM pc_aw_buckets LIMIT 1), 'window','chrome','2025-08-20 09:00:00+00',3600,'{}','evt_overlap_1'),
       (gen_random_uuid(), (SELECT id FROM pc_aw_buckets LIMIT 1), 'window','chrome','2025-08-20 09:00:00+00',3600,'{}','evt_overlap_2');
-- Expected: app-usage totalMinutes 60, Actual: 120; heatmap capped 60
