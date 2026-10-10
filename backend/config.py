import os

class BaseConfig(object):
    DIALCT = "mysql"
    DRITVER = "pymysql"

    HOST = os.getenv("DB_HOST", "mysql")
    PORT = os.getenv("DB_PORT", "3306")
    USERNAME = os.getenv("DB_USER", "root")
    PASSWORD = os.getenv("DB_PASSWORD", "123456")
    DBNAME = os.getenv("DB_NAME", "caimingxuan")

    REDIS_HOST = os.getenv("REDIS_HOST", "redis")
    REDIS_PORT = int(os.getenv("REDIS_PORT", "6379"))

    SQLALCHEMY_DATABASE_URI = (
        f"{DIALCT}+{DRITVER}://{USERNAME}:{PASSWORD}@{HOST}:{PORT}/{DBNAME}?charset=utf8"
    )
    SQLALCHEMY_TRACK_MODIFICATIONS = True
