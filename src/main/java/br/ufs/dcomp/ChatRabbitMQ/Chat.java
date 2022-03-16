package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;

import java.util.Scanner;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class Chat {

  public static void main(String[] argv) throws Exception 
  {
    
    Scanner sc = new Scanner(System.in);
    
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("3.91.68.223");
    factory.setUsername("jp");
    factory.setPassword("9910");
    factory.setVirtualHost("/");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    
    String QUEUE_NAME = "";
    while(QUEUE_NAME.isEmpty())
    {
      System.out.print("User: ");
      QUEUE_NAME = sc.nextLine();
    }
                      //(queue-name, durable, exclusive, auto-delete, params); 
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    Consumer consumer = new DefaultConsumer(channel) 
    {
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException 
      {
        //O QUE O RECEPTOR FARÁ QUANDO CHEGAR UMA MENSAGEM. "body" é o conteúdo da mensagem (representado em bytes)

        String message = new String(body, "UTF-8");
        System.out.println("\n"+message);
        //fazer uma variavel global indicando o prompt atual para imprimir aqui

      }
    };
                      //(queue-name, autoAck, consumer);    
    channel.basicConsume(QUEUE_NAME, true,    consumer);
    
    String destinatario = "";
    while(destinatario.isEmpty())
    {
        System.out.print(">> ");
        destinatario = sc.nextLine();
    }
    destinatario = destinatario.substring(1);
    while(true)
    {
        System.out.print("@"+destinatario+">> ");
        String msg = sc.nextLine();
        if(!msg.isEmpty())
        {
            char primeirochar = msg.charAt(0);
            int compare = Character.compare(primeirochar,'@');
            if(compare == 0)
            {
              destinatario = msg.substring(1);
            }
            else
            {/*
              String formato_data = "dd/MM/yyyy";
              String formato_hora = "HH:mm";
              DateTimeFormatter dtf_data = new DateTimeFormatter.ofPattern(formato_data);
              DateTimeFormatter dtf_hora = new DateTimeFormatter.ofPattern(formato_hora);
              
              LocalDateTime data = java.time.LocalDate.now();
              LocalDateTime hora = java.time.LocalTime.now();
              data_formatada = dtf_data.format(data);
              hora_formatada = dtf_hora.format(hora);*/
              
              //(21/09/2016 às 20:53) marciocosta diz: E aí, Tarcisio! Vamos sim!
              String data_formatada = java.time.LocalDate.now().toString();
              String hora_formatada = java.time.LocalTime.now().toString();
              
              String conteudo = new String(msg);
              msg = (  "("+data_formatada+" às "+hora_formatada+") "+QUEUE_NAME+" diz: "+conteudo   );
              channel.basicPublish("",destinatario,null,msg.getBytes("UTF-8"));
              System.out.println(" [x] Mensagem enviada: '" + msg + "'");
            }
        }
    }
  }
}