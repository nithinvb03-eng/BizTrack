# Manual database migrations

These scripts are not run automatically by Spring Boot.

For databases whose existing `users.phone` column is `NOT NULL`, select the
application database and run `make-user-phone-optional.sql` once. The migration
preserves the column's current data type and existing values while allowing new
users to omit a phone number.
