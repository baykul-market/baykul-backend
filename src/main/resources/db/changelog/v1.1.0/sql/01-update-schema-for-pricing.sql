-- Разрешаем null для наценки пользователя (для реализации fallback-логики)
ALTER TABLE public.users ALTER COLUMN markup_percentage DROP NOT NULL;

-- Добавляем колонки для расширенной настройки цен
ALTER TABLE public.price_config ADD COLUMN IF NOT EXISTS delivery_currency character varying(255);
ALTER TABLE public.price_config ADD COLUMN IF NOT EXISTS rounding_scale integer;
ALTER TABLE public.price_config ADD COLUMN IF NOT EXISTS rounding_mode character varying(255);
