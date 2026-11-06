SET @user_phone_column_type = (
    SELECT COLUMN_TYPE
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'phone'
);

SET @make_user_phone_optional = CONCAT(
    'ALTER TABLE `users` MODIFY COLUMN `phone` ',
    @user_phone_column_type,
    ' NULL'
);

PREPARE make_user_phone_optional_statement FROM @make_user_phone_optional;
EXECUTE make_user_phone_optional_statement;
DEALLOCATE PREPARE make_user_phone_optional_statement;
