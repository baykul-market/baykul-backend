-- Add can_see_actual_price column to users table
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS can_see_actual_price BOOLEAN NOT NULL DEFAULT false;

-- Make can_pay_later column NOT NULL
-- Set default value for any NULL records
UPDATE public.users
SET can_pay_later = false
WHERE can_pay_later IS NULL;

-- Alter the column to NOT NULL
ALTER TABLE public.users
    ALTER COLUMN can_pay_later SET NOT NULL;

-- Set a default value for future inserts
ALTER TABLE public.users
    ALTER COLUMN can_pay_later SET DEFAULT false;