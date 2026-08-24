-- Same window 00:00-01:00 insert session(30m) + summary fallback(60m) -> rows叠加 90m
INSERT INTO mobile_usage_sessions (id, user_id, device_id, package_name, start_utc, end_utc, duration_ms) VALUES (gen_random_uuid(), (SELECT id FROM users LIMIT 1), 'test_device','com.example','2025-08-20 00:00:00+00','2025-08-20 00:30:00+00',1800000);
INSERT INTO mobile_usage_summaries (id, user_id, device_id, window_start_utc, window_end_utc, total_time_foreground_ms) VALUES (gen_random_uuid(), (SELECT id FROM users LIMIT 1), 'test_device','2025-08-20 00:00:00+00','2025-08-20 01:00:00+00',3600000);
-- GET /analytics/overview & heatmap & charts all 90m
