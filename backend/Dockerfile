FROM python:3.10-slim

WORKDIR /app

COPY requirements.txt /app/
RUN pip install --no-cache-dir -r requirements.txt

COPY . /app/
RUN sed -i 's/\r$//' /app/wait-for-mysql.sh && chmod +x /app/wait-for-mysql.sh

EXPOSE 5000
CMD ["python", "app.py"]
