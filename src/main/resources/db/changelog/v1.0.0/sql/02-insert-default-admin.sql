-- Insert default admin user
INSERT INTO public.users (
    id,
    created_ts,
    updated_ts,
    login,
    password,
    email,
    phone_number,
    role,
    blocked,
    can_pay_later,
    markup_percentage
) VALUES (
    gen_random_uuid(),
    NOW(),
    NOW(),
    'admin',
    '$2a$12$/ahjlHcxWm14jmDd/Dv/V.EcNrLlx7.LezMWv7a3v46FoymVsdZ9G',
    'admin@example.com',
    NULL,
    'ADMIN',
    false,
    true,
    0
) ON CONFLICT (login) DO NOTHING;

-- Insert corresponding profile
INSERT INTO public.profile (
    id,
    created_ts,
    updated_ts,
    user_id,
    name,
    surname,
    patronymic
)
SELECT
    gen_random_uuid(),
    NOW(),
    NOW(),
    id,
    'Admin',
    'Admin',
    NULL
FROM public.users
WHERE login = 'admin'
ON CONFLICT DO NOTHING;

-- Insert balance for admin
INSERT INTO public.balance (
    id,
    created_ts,
    updated_ts,
    user_id,
    account,
    currency
)
SELECT
    gen_random_uuid(),
    NOW(),
    NOW(),
    id,
    0,
    'RUB'
FROM public.users
WHERE login = 'admin'
ON CONFLICT (user_id) DO NOTHING;

-- Insert cart for admin
INSERT INTO public.carts (
    id,
    created_ts,
    updated_ts,
    user_id
)
SELECT
    gen_random_uuid(),
    NOW(),
    NOW(),
    id
FROM public.users
WHERE login = 'admin'
ON CONFLICT (user_id) DO NOTHING;