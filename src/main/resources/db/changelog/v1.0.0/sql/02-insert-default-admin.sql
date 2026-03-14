-- Insert default admin user
INSERT INTO public.users (
    id,
    login,
    password,
    role,
    blocked,
    can_pay_later,
    markup_percentage,
    created_ts,
    updated_ts,
    email
) VALUES (
    gen_random_uuid(),
    'admin',
    '$2a$12$/ahjlHcxWm14jmDd/Dv/V.EcNrLlx7.LezMWv7a3v46FoymVsdZ9G',
    'ADMIN',
    false,
    true,
    0,
    NOW(),
    NOW(),
    'admin@example.com'
) ON CONFLICT (login) DO NOTHING;

-- Insert corresponding profile
INSERT INTO public.profile (
    id,
    user_id,
    name,
    surname,
    created_ts,
    updated_ts
)
SELECT
    gen_random_uuid(),
    id,
    'Admin',
    'Admin',
    NOW(),
    NOW()
FROM public.users
WHERE login = 'admin'
ON CONFLICT DO NOTHING;

-- Insert balance for admin
INSERT INTO public.balance (
    id,
    user_id,
    account,
    currency,
    created_ts,
    updated_ts
)
SELECT
    gen_random_uuid(),
    id,
    0,
    'USD',
    NOW(),
    NOW()
FROM public.users
WHERE login = 'admin'
ON CONFLICT (user_id) DO NOTHING;