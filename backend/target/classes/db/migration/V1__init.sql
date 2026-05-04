CREATE TABLE IF NOT EXISTS feature_flags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_percentage INT NOT NULL DEFAULT 0,
    targeted_user_ids TEXT DEFAULT '[]',
    environments TEXT DEFAULT '{"production":false,"staging":false,"development":true}',
    tags TEXT DEFAULT '[]',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS flag_evaluation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_key VARCHAR(100) NOT NULL,
    user_id VARCHAR(255),
    environment VARCHAR(50),
    result BOOLEAN NOT NULL,
    reason VARCHAR(50) NOT NULL,
    evaluated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flags_key ON feature_flags(flag_key);
CREATE INDEX idx_eval_logs_flag_key ON flag_evaluation_logs(flag_key);
CREATE INDEX idx_eval_logs_evaluated_at ON flag_evaluation_logs(evaluated_at DESC);

-- Seed sample flags
INSERT INTO feature_flags (flag_key, name, description, enabled, rollout_percentage, targeted_user_ids, environments) VALUES
('new-checkout-flow',    'New Checkout Flow',    'Redesigned checkout with single page',   true,  50, '["user-vip-001","user-vip-002"]', '{"production":true,"staging":true,"development":true}'),
('dark-mode',            'Dark Mode UI',         'Enable dark theme for all pages',         true,  100, '[]', '{"production":true,"staging":true,"development":true}'),
('ai-recommendations',  'AI Recommendations',   'ML-powered product recommendations',      false, 0,  '[]', '{"production":false,"staging":true,"development":true}'),
('beta-dashboard',       'Beta Dashboard',       'New analytics dashboard (beta users)',    true,  10, '["user-beta-001"]', '{"production":true,"staging":true,"development":true}');
