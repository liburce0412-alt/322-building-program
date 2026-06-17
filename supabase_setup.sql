-- CampusAI Supabase 建表脚本
-- 在 Supabase Dashboard → SQL Editor 中执行

CREATE TABLE IF NOT EXISTS public.profiles (
  id          UUID PRIMARY KEY REFERENCES auth.users(id),
  username    TEXT UNIQUE NOT NULL,
  avatar_url  TEXT,
  is_admin    BOOLEAN DEFAULT false,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.goods (
  id          BIGSERIAL PRIMARY KEY,
  user_id     UUID REFERENCES auth.users(id) NOT NULL,
  title       TEXT NOT NULL,
  description TEXT,
  tags        TEXT,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.messages (
  id          BIGSERIAL PRIMARY KEY,
  user_id     UUID REFERENCES auth.users(id) NOT NULL,
  username    TEXT NOT NULL,
  content     TEXT NOT NULL,
  is_approved BOOLEAN DEFAULT false,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.conversations (
  id          BIGSERIAL PRIMARY KEY,
  created_at  TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.conversation_participants (
  conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE,
  user_id         UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  PRIMARY KEY (conversation_id, user_id)
);

CREATE TABLE IF NOT EXISTS public.chat_messages (
  id              BIGSERIAL PRIMARY KEY,
  conversation_id BIGINT REFERENCES conversations(id) ON DELETE CASCADE NOT NULL,
  sender_id       UUID REFERENCES auth.users(id) NOT NULL,
  content         TEXT NOT NULL,
  is_approved     BOOLEAN DEFAULT true,
  created_at      TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE IF NOT EXISTS public.friends (
  user_id   UUID REFERENCES auth.users(id) NOT NULL,
  friend_id UUID REFERENCES auth.users(id) NOT NULL,
  status    TEXT DEFAULT 'pending',
  created_at TIMESTAMPTZ DEFAULT now(),
  PRIMARY KEY (user_id, friend_id)
);

CREATE TABLE IF NOT EXISTS public.achievements (
  id          BIGSERIAL PRIMARY KEY,
  name        TEXT NOT NULL,
  description TEXT NOT NULL,
  icon        TEXT NOT NULL,
  target      INT NOT NULL,
  category    TEXT NOT NULL
);

INSERT INTO public.achievements (id, name, description, icon, target, category) VALUES
  (1, '时间新手',    '累计专注 1 小时',       '🏆', 60,   'focus_time'),
  (2, '专注学者',    '累计专注 5 小时',       '🏆', 300,  'focus_time'),
  (3, '时间大师',    '累计专注 10 小时',      '🏆', 600,  'focus_time'),
  (4, '交易新手',    '发布第 1 个闲置物品',    '📦', 1,    'trades'),
  (5, '社交达人',    '发送第 10 条留言',      '💬', 10,   'messages'),
  (6, '社交之星',    '发送第 50 条留言',      '💬', 50,   'messages')
ON CONFLICT (id) DO NOTHING;

CREATE TABLE IF NOT EXISTS public.user_achievements (
  user_id        UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  achievement_id BIGINT REFERENCES achievements(id) ON DELETE CASCADE,
  progress       INT DEFAULT 0,
  unlocked_at    TIMESTAMPTZ,
  PRIMARY KEY (user_id, achievement_id)
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.goods ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
