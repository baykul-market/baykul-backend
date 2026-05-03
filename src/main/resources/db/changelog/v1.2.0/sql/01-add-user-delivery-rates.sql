-- Фаза 1: Добавляем колонку user_id (nullable) в таблицу delivery_cost_config
ALTER TABLE public.delivery_cost_config
    ADD COLUMN IF NOT EXISTS user_id UUID NULL;

-- Фаза 2: Добавляем внешний ключ на таблицу users
-- ON DELETE SET NULL: при удалении пользователя его тарифы становятся глобальными (user_id = NULL)
ALTER TABLE public.delivery_cost_config
    ADD CONSTRAINT fk_delivery_cost_config_user
        FOREIGN KEY (user_id)
            REFERENCES public.users(id)
            ON DELETE SET NULL;

-- Фаза 3: Добавляем составной индекс для оптимизации выборки
-- (user_id, minimum_sum DESC) соответствует типичному запросу: "найди правило для юзера X с максимальным порогом <= orderSum"
CREATE INDEX IF NOT EXISTS idx_delivery_cost_config_user_id_minimum_sum
    ON public.delivery_cost_config (user_id, minimum_sum DESC);
