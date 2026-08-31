-- =============================================
-- 小辽 M4 隐私与安全（云存储配套）
-- 隐私授权记录：云端存储/匿名统计需用户明确同意
-- =============================================
CREATE TABLE IF NOT EXISTS m4_privacy_consent (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL,
    consent_type VARCHAR(32) NOT NULL,
    granted     SMALLINT    DEFAULT 0,
    create_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT    DEFAULT 0
);
COMMENT ON TABLE m4_privacy_consent IS 'M4隐私授权记录';
COMMENT ON COLUMN m4_privacy_consent.consent_type IS '授权类型：cloud_storage=云端存储 anonymous_stats=匿名统计';
COMMENT ON COLUMN m4_privacy_consent.granted IS '0未授权 1已授权';
CREATE UNIQUE INDEX uk_m4_consent_user_type ON m4_privacy_consent(user_id, consent_type) WHERE deleted = 0;