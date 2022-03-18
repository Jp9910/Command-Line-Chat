package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;
import java.io.IOException;

import java.util.Scanner;
import java.time.LocalDate;
import java.time.LocalTime;

public class Chat {

  private static String prompt = new String("");
  
  public static void main(String[] argv) throws Exception 
  {
    
    Scanner sc = new Scanner(System.in);
    
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("23.22.251.211");
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
        System.out.print(prompt);
      }
    };
                      //(queue-name, autoAck, consumer);    
    channel.basicConsume(QUEUE_NAME, true,    consumer);
    
    String destinatario = "";
    while(destinatario.isEmpty())
    {
        prompt = ">> ";
        System.out.print(prompt);
        destinatario = sc.nextLine();
    }
    destinatario = destinatario.substring(1);
    while(true)
    {
        prompt = "@"+destinatario+">> ";
        System.out.print(prompt);
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
            {
              //formato: (21/09/2016 às 20:53) marciocosta diz: E aí, Tarcisio! Vamos sim!
              LocalDate data = java.time.LocalDate.now();
              int mes = data.getMonthValue();
              int dia = data.getDayOfMonth();
              String mes_formatado = mes < 10? "0"+String.valueOf(mes) : String.valueOf(mes);
              String dia_formatado = dia < 10? "0"+String.valueOf(dia) : String.valueOf(dia);
              String data_formatada = dia_formatado+"/"+mes_formatado+"/"+String.valueOf(data.getYear());
              LocalTime hora = java.time.LocalTime.now();
              String hora_formatada = String.valueOf(hora.getHour()) +":"+ String.valueOf(hora.getMinute());
              
              String conteudo = new String(msg);
              msg = (  "("+data_formatada+" às "+hora_formatada+") "+QUEUE_NAME+" diz: "+conteudo   );
              channel.basicPublish("",destinatario,null,msg.getBytes("UTF-8"));
            }
        }
    }
  }
}